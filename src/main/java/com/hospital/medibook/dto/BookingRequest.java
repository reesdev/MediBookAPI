package com.hospital.medibook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "Service ID wajib diisi")
    private Long serviceId;

    @NotNull(message = "Doctor ID wajib diisi")
    private Long doctorId;

    @NotNull(message = "Schedule ID wajib diisi")
    private Long scheduleId;

    @NotNull(message = "Tanggal booking wajib diisi")
    private LocalDate bookingDate;

    @NotBlank(message = "Keluhan wajib diisi")
    private String complaint;

    private MultipartFile referralFile;
}
