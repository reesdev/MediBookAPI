package com.hospital.medibook.service;

import com.hospital.medibook.constant.*;
import com.hospital.medibook.dto.BookingRequest;
import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.entity.*;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ConflictException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.*;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
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

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // Dapatkan identitas pasien terautentikasi
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil pasien tidak ditemukan"));

        // Cegah pendaftaran ganda menggunakan Redis lock
        String lockKey = "idempotency:booking:patient:" + patient.getId();
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new BadRequestException("Pendaftaran sedang diproses, harap tunggu beberapa saat.");
        }

        try {
            // Cek validitas tanggal jadwal
            if (request.getBookingDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Tanggal pendaftaran tidak boleh di masa lalu.");
            }

            // Validasi Double Booking (Pasien tidak boleh mendaftar jadwal dokter yang sama di hari yang sama)
            boolean isAlreadyBooked = bookingRepository.existsByPatientIdAndScheduleIdAndBookingDate(
                    patient.getId(), request.getScheduleId(), request.getBookingDate());
            if (isAlreadyBooked) {
                throw new ConflictException("Anda sudah pernah mendaftar pada jadwal dokter ini di tanggal tersebut.");
            }

            // Kunci baris jadwal di database (Pessimistic Lock)
            DoctorSchedule schedule = scheduleRepository.findByIdForUpdate(request.getScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Jadwal dokter tidak ditemukan atau sudah dihapus."));

            // Validasi kecocokan hari (DayOfWeek)
            if (request.getBookingDate().getDayOfWeek().getValue() != schedule.getDayOfWeek()) {
                throw new BadRequestException("Tanggal yang dipilih tidak sesuai dengan hari praktik dokter.");
            }

            // Validasi bookingTime
            if (request.getBookingTime() == null) {
                throw new BadRequestException("Jam kunjungan wajib dipilih.");
            }
            if (request.getBookingTime().isBefore(schedule.getStartTime()) || request.getBookingTime().isAfter(schedule.getEndTime()) || request.getBookingTime().equals(schedule.getEndTime())) {
                throw new BadRequestException("Jam kunjungan di luar jadwal praktek dokter.");
            }
            if (request.getBookingTime().getMinute() != 0) {
                throw new BadRequestException("Jam kunjungan harus pas per jam (misal 09:00, bukan 09:30).");
            }

            // Jika pendaftaran untuk hari ini, pastikan jam kunjungan belum terlewat
            if (request.getBookingDate().isEqual(LocalDate.now())) {
                if (java.time.LocalTime.now().isAfter(request.getBookingTime())) {
                    throw new BadRequestException("Waktu untuk slot jam " + request.getBookingTime() + " hari ini sudah lewat.");
                }
            }

            // Cek apakah slot jam ini sudah diambil orang lain
            boolean isSlotTaken = bookingRepository.existsByScheduleIdAndBookingDateAndBookingTimeAndStatusNot(
                    schedule.getId(), request.getBookingDate(), request.getBookingTime(), BookingStatus.CANCELLED);
            if (isSlotTaken) {
                throw new ConflictException("Mohon maaf, slot jam " + request.getBookingTime() + " sudah di-booking pasien lain.");
            }

            // Hitung kuota dinamis berdasarkan tanggal spesifik (Abaikan yang sudah dicancel)
            int currentBooked = bookingRepository.countByScheduleIdAndBookingDateAndStatusNot(
                    schedule.getId(), request.getBookingDate(), BookingStatus.CANCELLED);

            // Hitung nomor antrean selanjutnya
            int queueNumber = currentBooked + 1;

            // Generate Kode Booking
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = String.format("%04d", (int)(Math.random() * 10000));
            String bookingCode = "BK-" + datePart + "-" + randomPart;

            // Simpan entitas pendaftaran
            Booking booking = Booking.builder()
                    .bookingCode(bookingCode)
                    .patient(patient)
                    .service(schedule.getService())
                    .doctor(schedule.getDoctor())
                    .schedule(schedule)
                    .bookingDate(request.getBookingDate())
                    .bookingTime(request.getBookingTime())
                    .queueNumber(queueNumber)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .complaint(request.getComplaint())
                    .totalFee(schedule.getService().getBasePrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            Booking savedBooking = bookingRepository.save(booking);

            // Catat riwayat status (Audit log)
            BookingEvent event = BookingEvent.builder()
                    .booking(savedBooking)
                    .status(BookingStatus.PENDING_PAYMENT.name())
                    .eventType("BOOKING_CREATED")
                    .actor(Actor.PATIENT)
                    .detail("Pendaftaran booking berhasil dibuat. Menunggu pembayaran.")
                    .createdAt(LocalDateTime.now())
                    .build();
            eventRepository.save(event);

            // Simpan berkas rujukan jika diunggah
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
                    .message("Booking berhasil dibuat. Harap lakukan pembayaran dalam waktu 1 menit.")
                    .createdAt(savedBooking.getCreatedAt())
                    .serviceName(savedBooking.getService().getName())
                    .doctorName(savedBooking.getDoctor().getFullName())
                    .build();

        } finally {
            // Hapus Kunci Idempotency setelah transaksi selesai
            redisTemplate.delete(lockKey);
        }
    }

    public java.util.List<String> getAvailableTimeSlots(String scheduleId, LocalDate date) {
        DoctorSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Jadwal tidak ditemukan"));

        if (date.getDayOfWeek().getValue() != schedule.getDayOfWeek()) {
            return java.util.Collections.emptyList();
        }

        java.util.List<java.time.LocalTime> takenSlots = bookingRepository.findBookedTimes(scheduleId, date);
        java.util.List<String> available = new java.util.ArrayList<>();

        java.time.LocalTime current = schedule.getStartTime();
        java.time.LocalTime now = java.time.LocalTime.now();
        boolean isToday = date.isEqual(LocalDate.now());

        while (current.isBefore(schedule.getEndTime())) {
            // Jika hari ini dan jam slot sudah lewat, skip
            if (isToday && now.isAfter(current)) {
                current = current.plusHours(1);
                continue;
            }

            // Cek jika slot belum diambil
            if (!takenSlots.contains(current)) {
                available.add(current.toString());
            }

            current = current.plusHours(1);
        }

        return available;
    }

    private void handleFileUpload(Booking booking, MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") && 
                                        !contentType.startsWith("image/"))) {
                throw new BadRequestException("Hanya file PDF atau gambar yang diperbolehkan untuk dokumen rujukan.");
            }
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File destFile = new File(uploadDir, uniqueFileName);
            file.transferTo(destFile);
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
