package com.hospital.medibook.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private Integer queueNumber;
    private LocalDate bookingDate;
    private String status;
    private BigDecimal totalFee;
    private String message;
}
