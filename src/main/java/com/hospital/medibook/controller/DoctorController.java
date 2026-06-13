package com.hospital.medibook.controller;

import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Tag(name = "3. Doctor API", description = "Endpoint khusus untuk aktivitas Dokter (Ubah Status Booking)")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getUpcomingBookings() {
        List<BookingResponse> response = doctorService.getUpcomingBookings();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable("id") String bookingId, 
            @RequestParam("status") String status) {
        BookingResponse response = doctorService.updateBookingStatus(bookingId, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/bookings/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookingResponse> completeExamination(
            @PathVariable("id") String bookingId,
            @Valid @ModelAttribute com.hospital.medibook.dto.CompleteExaminationRequest request) {
        BookingResponse response = doctorService.completeExamination(bookingId, request);
        return ResponseEntity.ok(response);
    }
}
