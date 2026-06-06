package com.hospital.medibook.controller.api;

import com.hospital.medibook.dto.AuthResponse;
import com.hospital.medibook.dto.LoginRequest;
import com.hospital.medibook.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;

@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Endpoint untuk registrasi dan login pengguna")
public interface AuthApi {

    @Operation(summary = "Registrasi Pasien Baru",
               description = "Mendaftarkan akun baru dengan role PATIENT beserta profil pasien secara otomatis. NIK, username, dan email harus unik.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registrasi berhasil"),
        @ApiResponse(responseCode = "409", description = "Username, email, atau NIK sudah terdaftar"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal (field wajib kosong atau format salah)")
    })
    @PostMapping("/register")
    ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request);

    @Operation(summary = "Login & Dapatkan JWT Token",
               description = "Login dengan username dan password. Jika berhasil, mengembalikan JWT Bearer Token yang wajib disertakan di header Authorization untuk semua endpoint yang memerlukan autentikasi.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token JWT dikembalikan"),
        @ApiResponse(responseCode = "401", description = "Username atau password salah"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);
}
