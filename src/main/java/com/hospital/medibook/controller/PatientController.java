package com.hospital.medibook.controller;

import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.entity.Patient;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.BookingRepository;
import com.hospital.medibook.repository.PatientRepository;
import com.hospital.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan."));
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil pasien tidak ditemukan."));

        List<BookingResponse> bookings = bookingRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(b -> BookingResponse.builder()
                        .id(b.getId())
                        .bookingCode(b.getBookingCode())
                        .queueNumber(b.getQueueNumber())
                        .bookingDate(b.getBookingDate())
                        .status(b.getStatus().name())
                        .totalFee(b.getTotalFee())
                        .message(b.getComplaint())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(bookings);
    }
}
