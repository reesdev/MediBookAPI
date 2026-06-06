package com.hospital.medibook.controller;

import com.hospital.medibook.dto.*;
import com.hospital.medibook.service.BookingService;
import com.hospital.medibook.service.PaymentService;
import com.hospital.medibook.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.beans.PropertyEditorSupport;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    // Handle empty string from Swagger for MultipartFile
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
    public ResponseEntity<BookingResponse> createBooking(@Valid @ModelAttribute BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payBooking(@PathVariable("id") Long bookingId, @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable("id") Long bookingId, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
