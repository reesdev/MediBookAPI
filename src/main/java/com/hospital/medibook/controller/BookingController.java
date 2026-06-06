package com.hospital.medibook.controller;

import com.hospital.medibook.dto.*;
import com.hospital.medibook.service.BookingService;
import com.hospital.medibook.service.PaymentService;
import com.hospital.medibook.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    public BookingController(BookingService bookingService,
                             PaymentService paymentService,
                             ReviewService reviewService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<BookingResponse> createBooking(@Valid @ModelAttribute BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payBooking(
            @PathVariable("id") Long bookingId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> submitReview(
            @PathVariable("id") Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
