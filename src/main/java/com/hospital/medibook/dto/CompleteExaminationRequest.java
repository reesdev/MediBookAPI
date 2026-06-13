package com.hospital.medibook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteExaminationRequest {

    @NotBlank(message = "Hasil diagnosa wajib diisi")
    @Schema(description = "Hasil diagnosa dokter", example = "Pasien mengalami gejala flu ringan dan radang tenggorokan.")
    private String diagnosis;

    @Schema(description = "Catatan resep (opsional jika mengunggah file resep)", example = "Paracetamol 500mg 3x1 sesudah makan")
    private String prescriptionNotes;

    @Schema(description = "File digital surat resep dalam format gambar/PDF (opsional)")
    private MultipartFile prescriptionFile;
}
