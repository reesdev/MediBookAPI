package com.hospital.medibook.service;

import com.hospital.medibook.constant.BookingStatus;
import com.hospital.medibook.dto.ReviewRequest;
import com.hospital.medibook.dto.ReviewResponse;
import com.hospital.medibook.entity.Booking;
import com.hospital.medibook.entity.Patient;
import com.hospital.medibook.entity.Review;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.PatientRepository;
import com.hospital.medibook.repository.ReviewRepository;
import com.hospital.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public ReviewResponse submitReview(String bookingId, ReviewRequest request) {
        // Identifikasi pasien
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil pasien tidak ditemukan."));

        // Cari pendaftaran terkait
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking tidak ditemukan."));

        // Pastikan pendaftaran milik pasien ini
        if (!booking.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestException("Anda tidak berhak memberikan ulasan untuk pendaftaran ini.");
        }

        // Review hanya bisa dilakukan setelah selesai
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Ulasan hanya dapat diberikan setelah status pelayanan selesai (COMPLETED).");
        }

        // Cek apakah sudah pernah memberikan review
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new BadRequestException("Anda sudah memberikan ulasan untuk pendaftaran ini.");
        }

        // Simpan ulasan ke database
        Review review = Review.builder()
                .booking(booking)
                .patient(patient)
                .doctor(booking.getDoctor())
                .service(booking.getService())
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .createdAt(LocalDateTime.now())
                .build();
        Review savedReview = reviewRepository.save(review);

        return ReviewResponse.builder()
                .reviewId(savedReview.getId())
                .bookingCode(booking.getBookingCode())
                .rating(savedReview.getRating())
                .message("Ulasan berhasil dikirimkan. Terima kasih atas masukan Anda.")
                .build();
    }
}
