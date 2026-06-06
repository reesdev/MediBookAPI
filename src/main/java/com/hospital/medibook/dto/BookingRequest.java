package com.hospital.medibook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Form data untuk membuat booking. Kirim sebagai multipart/form-data.")
public class BookingRequest {

    @NotNull(message = "Service ID wajib diisi")
    @Schema(description = "ID Layanan rumah sakit (dari GET /api/services)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long serviceId;

    @NotNull(message = "Doctor ID wajib diisi")
    @Schema(description = "ID Dokter (dari GET /api/doctors)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long doctorId;

    @NotNull(message = "Schedule ID wajib diisi")
    @Schema(description = "ID Jadwal dokter (dari GET /api/schedules)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long scheduleId;

    @NotNull(message = "Tanggal booking wajib diisi")
    @Schema(description = "Tanggal kunjungan (format YYYY-MM-DD, tidak boleh masa lalu)", example = "2026-06-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate bookingDate;

    @NotBlank(message = "Keluhan wajib diisi")
    @Schema(description = "Keluhan utama pasien", example = "Anak demam dan batuk pilek sejak 2 hari", requiredMode = Schema.RequiredMode.REQUIRED)
    private String complaint;

    @Schema(description = "File surat rujukan (opsional — PDF atau gambar, maks ~5MB). Boleh dikosongkan.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", format = "binary")
    private MultipartFile referralFile;
}
