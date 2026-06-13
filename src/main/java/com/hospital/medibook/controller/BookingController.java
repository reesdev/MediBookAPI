package com.hospital.medibook.controller;

import com.hospital.medibook.dto.*;
import com.hospital.medibook.service.BookingService;
import com.hospital.medibook.service.PaymentService;
import com.hospital.medibook.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "2. Patient API", description = "Endpoint untuk aktivitas Pasien (Booking, Bayar, Review)")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                // string kosong atau null → set referralFile ke null (opsional, skip)
                setValue(null);
            }
        });
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Buat Booking Baru", description = "Endpoint untuk mendaftar pasien dengan upload file rujukan (opsional).")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BookingRequest.class)
            )
    )
    public ResponseEntity<BookingResponse> createBooking(@Parameter(hidden = true) @Valid @ModelAttribute BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payBooking(@PathVariable("id") String bookingId, @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable("id") String bookingId, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/available-times")
    public ResponseEntity<List<String>> getAvailableTimes(
            @RequestParam("scheduleId") String scheduleId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<String> times = bookingService.getAvailableTimeSlots(scheduleId, date);
        return ResponseEntity.ok(times);
    }
}
