package com.hospital.medibook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "Metode pembayaran wajib diisi")
    private String paymentMethod;

    @NotNull(message = "Jumlah pembayaran wajib diisi")
    private BigDecimal amount;
}
