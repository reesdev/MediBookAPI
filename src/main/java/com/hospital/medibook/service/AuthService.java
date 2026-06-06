package com.hospital.medibook.service;

import com.hospital.medibook.config.JwtUtils;
import com.hospital.medibook.constant.Gender;
import com.hospital.medibook.constant.Role;
import com.hospital.medibook.dto.AuthResponse;
import com.hospital.medibook.dto.LoginRequest;
import com.hospital.medibook.dto.RegisterRequest;
import com.hospital.medibook.entity.Patient;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ConflictException;
import com.hospital.medibook.repository.PatientRepository;
import com.hospital.medibook.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username sudah terdaftar");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email sudah terdaftar");
        }
        if (patientRepository.existsByNik(request.getNik())) {
            throw new ConflictException("NIK sudah terdaftar");
        }

        // 1. Buat User
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.PATIENT)
                .isDeleted(false)
                .build();
        User savedUser = userRepository.save(user);

        // 2. Buat Patient
        Gender gender;
        try {
            gender = Gender.fromValue(request.getGender());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Jenis kelamin tidak valid. Harap pilih 'Laki-laki' atau 'Perempuan'.");
        }

        Patient patient = Patient.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .nik(request.getNik())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .gender(gender)
                .address(request.getAddress())
                .isDeleted(false)
                .build();
        patientRepository.save(patient);

        return savedUser;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Username atau password salah"));

        if (user.isDeleted()) {
            throw new BadRequestException("Akun Anda telah dinonaktifkan.");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .username(user.getUsername())
                .build();
    }
}
