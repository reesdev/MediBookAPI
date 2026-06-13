package com.hospital.medibook.service;

import com.hospital.medibook.constant.Actor;
import com.hospital.medibook.constant.BookingStatus;
import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.entity.Booking;
import com.hospital.medibook.entity.BookingEvent;
import com.hospital.medibook.entity.Doctor;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.BookingEventRepository;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.DoctorRepository;
import com.hospital.medibook.repository.MedicalDocumentRepository;
import com.hospital.medibook.repository.UserRepository;
import com.hospital.medibook.dto.CompleteExaminationRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final BookingRepository bookingRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final BookingEventRepository eventRepository;
    private final MedicalDocumentRepository documentRepository;

    public List<BookingResponse> getUpcomingBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil dokter tidak ditemukan."));

        List<Booking> bookings = bookingRepository.findByDoctorIdAndBookingDateGreaterThanEqualOrderByBookingDateAscQueueNumberAsc(
                doctor.getId(), LocalDate.now()
        );

        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateBookingStatus(String bookingId, String statusStr) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil dokter tidak ditemukan."));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking tidak ditemukan."));

        if (!booking.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("Anda tidak berhak memperbarui status pendaftaran ini.");
        }

        BookingStatus newStatus;
        try {
            newStatus = BookingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status tidak valid. Harap pilih: IN_PROGRESS atau COMPLETED.");
        }

        BookingStatus oldStatus = booking.getStatus();
        
        // Validasi Transisi Status:
        // - CONFIRMED -> IN_PROGRESS
        // - IN_PROGRESS -> COMPLETED
        if (oldStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.IN_PROGRESS) {
            booking.setStatus(newStatus);
        } else if (oldStatus == BookingStatus.IN_PROGRESS && newStatus == BookingStatus.COMPLETED) {
            booking.setStatus(newStatus);
        } else {
            throw new BadRequestException("Transisi status tidak valid. Status saat ini: " + oldStatus + ", Target status: " + newStatus);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Catat Audit Event
        BookingEvent event = BookingEvent.builder()
                .booking(savedBooking)
                .status(newStatus.name())
                .eventType("STATUS_UPDATED")
                .actor(Actor.DOCTOR)
                .detail("Status pemeriksaan diubah oleh dokter dari " + oldStatus + " menjadi " + newStatus)
                .createdAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        return mapToBookingResponse(savedBooking);
    }

    @Transactional
    public BookingResponse completeExamination(String bookingId, CompleteExaminationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil dokter tidak ditemukan."));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking tidak ditemukan."));

        if (!booking.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("Anda tidak berhak memperbarui status pendaftaran ini.");
        }

        BookingStatus oldStatus = booking.getStatus();
        if (oldStatus != BookingStatus.IN_PROGRESS) {
            throw new BadRequestException("Hanya booking dengan status IN_PROGRESS yang dapat diselesaikan.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setDiagnosis(request.getDiagnosis());
        booking.setPrescriptionNotes(request.getPrescriptionNotes());

        Booking savedBooking = bookingRepository.save(booking);

        BookingEvent event = BookingEvent.builder()
                .booking(savedBooking)
                .status(BookingStatus.COMPLETED.name())
                .eventType("EXAMINATION_COMPLETED")
                .actor(Actor.DOCTOR)
                .detail("Pemeriksaan selesai. Hasil diagnosa: " + request.getDiagnosis())
                .createdAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        MultipartFile file = request.getPrescriptionFile();
        if (file != null && !file.isEmpty()) {
            handlePrescriptionUpload(savedBooking, file);
        }

        return mapToBookingResponse(savedBooking);
    }

    private void handlePrescriptionUpload(Booking booking, MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") && 
                                        !contentType.startsWith("image/"))) {
                throw new BadRequestException("Hanya file PDF atau gambar yang diperbolehkan untuk dokumen resep.");
            }
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File destFile = new File(uploadDir, uniqueFileName);
            file.transferTo(destFile);
            
            com.hospital.medibook.entity.MedicalDocument document = com.hospital.medibook.entity.MedicalDocument.builder()
                    .booking(booking)
                    .filePath("uploads/" + uniqueFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(contentType)
                    .uploadedBy(com.hospital.medibook.constant.UploadedBy.DOCTOR)
                    .documentType(com.hospital.medibook.constant.DocumentType.PRESCRIPTION)
                    .createdAt(LocalDateTime.now())
                    .build();
            documentRepository.save(document);
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengunggah file resep.", e);
        }
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .queueNumber(booking.getQueueNumber())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus().name())
                .totalFee(booking.getTotalFee())
                .message("Status booking diperbarui.")
                .createdAt(booking.getCreatedAt())
                .serviceName(booking.getService().getName())
                .doctorName(booking.getDoctor().getFullName())
                .build();
    }
}
