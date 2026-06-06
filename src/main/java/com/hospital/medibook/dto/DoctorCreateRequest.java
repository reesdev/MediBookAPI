package com.hospital.medibook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreateRequest {
    @NotNull(message = "User ID wajib diisi")
    private Long userId;

    @NotBlank(message = "Nama lengkap wajib diisi")
    private String fullName;

    @NotBlank(message = "Spesialisasi wajib diisi")
    private String specialization;

    @NotBlank(message = "SIP wajib diisi")
    private String sip;

    @NotBlank(message = "Nomor telepon wajib diisi")
    private String phone;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;
}
