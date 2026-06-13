package com.hospital.medibook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateRequest {
    @NotNull(message = "Doctor ID wajib diisi")
    private String doctorId;

    @NotNull(message = "Service ID wajib diisi")
    private String serviceId;

    @NotNull(message = "Hari dalam seminggu wajib diisi (1 = Senin, 7 = Minggu)")
    @Min(value = 1, message = "Hari minimal 1 (Senin)")
    @Max(value = 7, message = "Hari maksimal 7 (Minggu)")
    private Integer dayOfWeek;

    @NotNull(message = "Waktu mulai wajib diisi")
    private LocalTime startTime;

    @NotNull(message = "Waktu selesai wajib diisi")
    private LocalTime endTime;

    @NotNull(message = "Jumlah maksimal pasien wajib diisi")
    @Min(value = 1, message = "Jumlah maksimal pasien minimal 1")
    private Integer maxPatients;
}
