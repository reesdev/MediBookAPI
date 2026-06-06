package com.hospital.medibook.controller;

import com.hospital.medibook.controller.api.BookingApi;
import com.hospital.medibook.dto.*;
import com.hospital.medibook.service.BookingService;
import com.hospital.medibook.service.PaymentService;
import com.hospital.medibook.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.beans.PropertyEditorSupport;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingApi {

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

    @Override
    public ResponseEntity<BookingResponse> createBooking(BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<PaymentResponse> payBooking(Long bookingId, PaymentRequest request) {
        PaymentResponse response = paymentService.payBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ReviewResponse> submitReview(Long bookingId, ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
