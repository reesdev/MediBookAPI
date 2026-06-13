package com.hospital.medibook.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private String reviewId;
    private String bookingCode;
    private Integer rating;
    private String message;
}
