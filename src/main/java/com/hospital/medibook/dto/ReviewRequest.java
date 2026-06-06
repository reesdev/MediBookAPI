package com.hospital.medibook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    @NotNull(message = "Rating wajib diisi")
    @Min(value = 1, message = "Rating minimal 1 bintang")
    @Max(value = 5, message = "Rating maksimal 5 bintang")
    private Integer rating;

    @Size(max = 500, message = "Ulasan maksimal 500 karakter")
    private String reviewText;
}
