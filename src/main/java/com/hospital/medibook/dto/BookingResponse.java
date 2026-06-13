package com.hospital.medibook.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private String id;
    private String bookingCode;
    private Integer queueNumber;
    private LocalDate bookingDate;
    private String status;
    private BigDecimal totalFee;
    private String message;
    private String serviceName;
    private String doctorName;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalTime bookingTime;
}
