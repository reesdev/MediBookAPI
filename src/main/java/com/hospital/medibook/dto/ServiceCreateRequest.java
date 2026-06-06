package com.hospital.medibook.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCreateRequest {
    @NotBlank(message = "Nama layanan wajib diisi")
    private String name;

    @NotBlank(message = "Kategori layanan wajib diisi (POLIKLINIK atau PENUNJANG_MEDIS)")
    private String category;

    private String description;

    @NotNull(message = "Harga dasar wajib diisi")
    @DecimalMin(value = "0.0", inclusive = false, message = "Harga dasar harus lebih dari 0")
    private BigDecimal basePrice;
}
