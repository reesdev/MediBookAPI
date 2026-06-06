package com.hospital.medibook.controller;

import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getTodayBookings() {
        List<BookingResponse> response = doctorService.getTodayBookings();
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
