package com.hospital.medibook.service;

import com.hospital.medibook.constant.*;
import com.hospital.medibook.dto.BookingRequest;
import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.entity.*;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ConflictException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final BookingEventRepository eventRepository;
    private final MedicalDocumentRepository documentRepository;
    private final StringRedisTemplate redisTemplate;

    public BookingService(BookingRepository bookingRepository,
                          DoctorScheduleRepository scheduleRepository,
                          DoctorRepository doctorRepository,
                          HospitalServiceRepository serviceRepository,
                          UserRepository userRepository,
                          PatientRepository patientRepository,
                          BookingEventRepository eventRepository,
                          MedicalDocumentRepository documentRepository,
                          StringRedisTemplate redisTemplate) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.eventRepository = eventRepository;
        this.documentRepository = documentRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // 1. Dapatkan pasien yang sedang terautentikasi
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil pasien tidak ditemukan"));

        // 2. Idempotency Lock dengan Redis
        String lockKey = "idempotency:booking:patient:" + patient.getId();
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new BadRequestException("Pendaftaran sedang diproses, harap tunggu beberapa saat.");
        }

        try {
            // 3. Validasi Tanggal Booking (Minimal hari ini atau hari esok)
            if (request.getBookingDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Tanggal pendaftaran tidak boleh di masa lalu.");
            }

            // 4. Lakukan Pessimistic Lock pada Jadwal Dokter
            DoctorSchedule schedule = scheduleRepository.findByIdForUpdate(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Jadwal dokter tidak ditemukan atau sudah dihapus."));

            // 5. Validasi Kecocokan Jadwal dengan Dokter & Layanan
            if (!schedule.getDoctor().getId().equals(request.getDoctorId())) {
                throw new BadRequestException("Jadwal tidak cocok dengan dokter yang dipilih.");
            }
            if (!schedule.getService().getId().equals(request.getServiceId())) {
                throw new BadRequestException("Jadwal tidak cocok dengan layanan yang dipilih.");
            }

            // 6. Validasi Sisa Kuota
            if (schedule.getBookedCount() >= schedule.getMaxPatients()) {
                throw new ConflictException("Kuota dokter untuk jadwal yang dipilih sudah penuh.");
            }

            // 7. Hitung Nomor Antrean
            int queueNumber = schedule.getBookedCount() + 1;
            schedule.setBookedCount(queueNumber);
            scheduleRepository.save(schedule);

            // 8. Generate Kode Booking Unik: BK-yyyyMMdd-XXXX (XXXX dari serial queue/random)
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = String.format("%04d", (int)(Math.random() * 10000));
            String bookingCode = "BK-" + datePart + "-" + randomPart;

            // 9. Buat Entitas Booking
            Booking booking = Booking.builder()
                    .bookingCode(bookingCode)
                    .patient(patient)
                    .service(schedule.getService())
                    .doctor(schedule.getDoctor())
                    .schedule(schedule)
                    .bookingDate(request.getBookingDate())
                    .queueNumber(queueNumber)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .complaint(request.getComplaint())
                    .totalFee(schedule.getService().getBasePrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Booking savedBooking = bookingRepository.save(booking);

            // 10. Catat Booking Event
            BookingEvent event = BookingEvent.builder()
                    .booking(savedBooking)
                    .status(BookingStatus.PENDING_PAYMENT.name())
                    .eventType("BOOKING_CREATED")
                    .actor(Actor.PATIENT)
                    .detail("Pendaftaran booking berhasil dibuat. Menunggu pembayaran.")
                    .createdAt(LocalDateTime.now())
                    .build();
            eventRepository.save(event);

            // 11. Tangani Upload File Rujukan (Jika Ada)
            MultipartFile file = request.getReferralFile();
            if (file != null && !file.isEmpty()) {
                handleFileUpload(savedBooking, file);
            }

            return BookingResponse.builder()
                    .id(savedBooking.getId())
                    .bookingCode(savedBooking.getBookingCode())
                    .queueNumber(savedBooking.getQueueNumber())
                    .bookingDate(savedBooking.getBookingDate())
                    .status(savedBooking.getStatus().name())
                    .totalFee(savedBooking.getTotalFee())
                    .message("Booking berhasil dibuat. Harap lakukan pembayaran dalam waktu 15 menit.")
                    .build();

        } finally {
            // Hapus Kunci Idempotency setelah transaksi selesai
            redisTemplate.delete(lockKey);
        }
    }

    private void handleFileUpload(Booking booking, MultipartFile file) {
        try {
            // Validasi jenis file (PDF atau Gambar)
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") && 
                                        !contentType.startsWith("image/"))) {
                throw new BadRequestException("Hanya file PDF atau gambar yang diperbolehkan untuk dokumen rujukan.");
            }

            // Buat folder uploads jika belum ada
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate nama file unik
            String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File destFile = new File(uploadDir, uniqueFileName);
            file.transferTo(destFile);

            // Simpan metadata ke tabel medical_documents
            MedicalDocument document = MedicalDocument.builder()
                    .booking(booking)
                    .filePath("uploads/" + uniqueFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(contentType)
                    .uploadedBy(UploadedBy.PATIENT)
                    .documentType(DocumentType.REFERRAL_LETTER)
                    .createdAt(LocalDateTime.now())
                    .build();
            documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("Gagal mengunggah file dokumen rujukan.", e);
        }
    }
}
