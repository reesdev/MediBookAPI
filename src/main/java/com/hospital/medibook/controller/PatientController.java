package com.hospital.medibook.controller;

import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@Tag(name = "2. Patient API", description = "Endpoint untuk aktivitas Pasien (Melihat riwayat booking)")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        List<BookingResponse> bookings = patientService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }
}
