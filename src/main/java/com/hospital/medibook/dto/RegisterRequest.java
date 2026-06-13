package com.hospital.medibook.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username wajib diisi")
    @Size(min = 4, max = 50, message = "Username minimal 4 karakter dan maksimal 50 karakter")
    private String username;

    @NotBlank(message = "Password wajib diisi")
    @Size(min = 6, max = 100, message = "Password minimal 6 karakter")
    private String password;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    @Size(max = 100, message = "Email maksimal 100 karakter")
    private String email;

    @NotBlank(message = "Nama lengkap wajib diisi")
    @Size(max = 100, message = "Nama lengkap maksimal 100 karakter")
    private String fullName;

    @NotBlank(message = "NIK wajib diisi")
    @Size(min = 16, max = 20, message = "NIK minimal 16 karakter dan maksimal 20 karakter")
    private String nik;

    @NotBlank(message = "Nomor telepon wajib diisi")
    @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
    private String phone;

    @NotNull(message = "Tanggal lahir wajib diisi")
    private LocalDate birthDate;

    @NotBlank(message = "Jenis kelamin wajib diisi")
    private String gender;

    private String address;
}
