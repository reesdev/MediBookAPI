package com.hospital.medibook.controller;

import com.hospital.medibook.controller.api.DoctorApi;
import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorController implements DoctorApi {

    private final DoctorService doctorService;

    @Override
    public ResponseEntity<List<BookingResponse>> getTodayBookings(java.time.LocalDate date) {
        java.time.LocalDate targetDate = (date != null) ? date : java.time.LocalDate.now();
        List<BookingResponse> response = doctorService.getBookingsByDate(targetDate);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<BookingResponse> updateBookingStatus(Long bookingId, String status) {
        BookingResponse response = doctorService.updateBookingStatus(bookingId, status);
        return ResponseEntity.ok(response);
    }
}
