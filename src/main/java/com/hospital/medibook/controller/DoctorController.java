package com.hospital.medibook.controller;

import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getTodayBookings(
            @RequestParam(value = "date", required = false) 
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        java.time.LocalDate targetDate = (date != null) ? date : java.time.LocalDate.now();
        List<BookingResponse> response = doctorService.getBookingsByDate(targetDate);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable("id") Long bookingId, 
            @RequestParam("status") String status) {
        BookingResponse response = doctorService.updateBookingStatus(bookingId, status);
        return ResponseEntity.ok(response);
    }
}
