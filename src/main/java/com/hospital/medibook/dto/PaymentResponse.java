package com.hospital.medibook.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String bookingCode;
    private String status;
    private LocalDateTime paymentTime;
    private String transactionCode;
    private String message;
}
