package com.hospital.medibook.controller;

import com.hospital.medibook.dto.AuthResponse;
import com.hospital.medibook.dto.LoginRequest;
import com.hospital.medibook.dto.RegisterRequest;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Endpoint untuk registrasi dan login pengguna")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Registrasi Pasien Baru",
               description = "Mendaftarkan akun baru dengan role PATIENT beserta profil pasien secara otomatis. NIK, username, dan email harus unik.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registrasi berhasil"),
        @ApiResponse(responseCode = "409", description = "Username, email, atau NIK sudah terdaftar"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal (field wajib kosong atau format salah)")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = authService.register(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("userId", registeredUser.getId());
        response.put("username", registeredUser.getUsername());
        response.put("role", registeredUser.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login & Dapatkan JWT Token",
               description = "Login dengan username dan password. Jika berhasil, mengembalikan JWT Bearer Token yang wajib disertakan di header Authorization untuk semua endpoint yang memerlukan autentikasi.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token JWT dikembalikan"),
        @ApiResponse(responseCode = "401", description = "Username atau password salah"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
