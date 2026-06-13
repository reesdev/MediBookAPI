package com.hospital.medibook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingRequest {

    @NotNull(message = "Schedule ID wajib diisi")
    @Schema(description = "ID Jadwal dokter (dari GET /api/schedules)", example = "123e4567-e89b-12d3-a456-426614174003", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scheduleId;

    @NotNull(message = "Tanggal booking wajib diisi")
    @Schema(description = "Tanggal kunjungan (format YYYY-MM-DD, tidak boleh masa lalu)", example = "2026-06-10", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bookingDate;

    @NotNull(message = "Jam booking wajib diisi")
    @Schema(description = "Jam kunjungan (format HH:mm)", example = "09:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime bookingTime;

    @NotBlank(message = "Keluhan wajib diisi")
    @Schema(description = "Keluhan utama pasien", example = "Anak demam dan batuk pilek sejak 2 hari", requiredMode = Schema.RequiredMode.REQUIRED)
    private String complaint;

    @Schema(description = "File surat rujukan (opsional — PDF atau gambar, maks ~5MB). Boleh dikosongkan.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private MultipartFile referralFile;
}
