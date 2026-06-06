package com.hospital.medibook.controller;

import com.hospital.medibook.controller.api.AuthApi;
import com.hospital.medibook.dto.AuthResponse;
import com.hospital.medibook.dto.LoginRequest;
import com.hospital.medibook.dto.RegisterRequest;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<Map<String, Object>> register(RegisterRequest request) {
        User registeredUser = authService.register(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("userId", registeredUser.getId());
        response.put("username", registeredUser.getUsername());
        response.put("role", registeredUser.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
