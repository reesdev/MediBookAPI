package com.hospital.medibook.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalServiceResponse {
    private Long id;
    private String name;
    private String category;
    private String description;
    private BigDecimal basePrice;
}
