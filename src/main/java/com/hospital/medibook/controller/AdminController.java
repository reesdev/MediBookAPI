package com.hospital.medibook.controller;

import com.hospital.medibook.constant.Role;
import com.hospital.medibook.constant.ServiceCategory;
import com.hospital.medibook.dto.DoctorCreateRequest;
import com.hospital.medibook.dto.ScheduleCreateRequest;
import com.hospital.medibook.dto.ServiceCreateRequest;
import com.hospital.medibook.entity.Doctor;
import com.hospital.medibook.entity.DoctorSchedule;
import com.hospital.medibook.entity.HospitalService;
import com.hospital.medibook.entity.User;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.exception.ResourceNotFoundException;
import com.hospital.medibook.repository.DoctorRepository;
import com.hospital.medibook.repository.DoctorScheduleRepository;
import com.hospital.medibook.repository.HospitalServiceRepository;
import com.hospital.medibook.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "4. Master Data (ADMIN Only)", description = "Endpoint manajemen master data: tambah dokter, layanan, dan jadwal. Wajib login sebagai ADMIN.")
public class AdminController {

    private final DoctorRepository doctorRepository;
    private final HospitalServiceRepository serviceRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public AdminController(DoctorRepository doctorRepository,
                           HospitalServiceRepository serviceRepository,
                           DoctorScheduleRepository scheduleRepository,
                           UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.serviceRepository = serviceRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    @Operation(
        summary = "Tambah Profil Dokter Baru",
        description = """
            Admin menambahkan profil dokter baru.
            
            - `userId` harus merujuk ke user yang sudah ada dengan role `DOCTOR`.
            - SIP (Surat Izin Praktek) harus unik.
            - Untuk membuat user berdokter, daftarkan user DOCTOR terlebih dahulu via seeder atau endpoint internal.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profil dokter berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "User tidak ditemukan, role bukan DOCTOR, atau SIP sudah terdaftar"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/doctors")
    @Transactional
    public ResponseEntity<Map<String, Object>> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan ID: " + request.getUserId()));

        if (user.getRole() != Role.DOCTOR) {
            throw new BadRequestException("User yang dipilih tidak memiliki role DOCTOR.");
        }

        if (doctorRepository.existsBySip(request.getSip())) {
            throw new BadRequestException("SIP Dokter sudah terdaftar.");
        }

        Doctor doctor = Doctor.builder()
                .user(user)
                .fullName(request.getFullName())
                .specialization(request.getSpecialization())
                .sip(request.getSip())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isDeleted(false)
                .build();
        Doctor savedDoctor = doctorRepository.save(doctor);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profil dokter berhasil dibuat.");
        response.put("doctorId", savedDoctor.getId());
        response.put("fullName", savedDoctor.getFullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Tambah Layanan Rumah Sakit Baru",
        description = """
            Admin menambahkan layanan rumah sakit baru ke katalog.
            
            - `category` harus berupa salah satu: `POLIKLINIK` atau `PENUNJANG_MEDIS` (case-insensitive).
            - `basePrice` adalah harga dasar layanan dalam Rupiah.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Layanan berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "Kategori tidak valid"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/services")
    public ResponseEntity<Map<String, Object>> createService(@Valid @RequestBody ServiceCreateRequest request) {
        ServiceCategory category;
        try {
            category = ServiceCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Kategori tidak valid. Pilihan: POLIKLINIK atau PENUNJANG_MEDIS.");
        }

        HospitalService service = HospitalService.builder()
                .name(request.getName())
                .category(category)
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .isDeleted(false)
                .build();
        HospitalService savedService = serviceRepository.save(service);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Layanan rumah sakit berhasil dibuat.");
        response.put("serviceId", savedService.getId());
        response.put("name", savedService.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Tambah Jadwal Dokter Baru",
        description = """
            Admin menambahkan jadwal praktik dokter.
            
            - `doctorId` harus merujuk ke profil dokter yang sudah ada.
            - `serviceId` harus merujuk ke layanan yang sudah ada.
            - `dayOfWeek`: 1=Senin, 2=Selasa, 3=Rabu, 4=Kamis, 5=Jumat, 6=Sabtu, 7=Minggu.
            - `startTime` dan `endTime` menggunakan format `HH:mm:ss` (contoh: `08:00:00`).
            - `maxPatients` adalah kuota maksimal pasien per jadwal.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Jadwal dokter berhasil dibuat"),
        @ApiResponse(responseCode = "401", description = "Token JWT tidak valid atau tidak dikirim"),
        @ApiResponse(responseCode = "403", description = "Role tidak memiliki akses (bukan ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Dokter atau layanan tidak ditemukan"),
        @ApiResponse(responseCode = "422", description = "Validasi form gagal")
    })
    @PostMapping("/schedules")
    public ResponseEntity<Map<String, Object>> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Dokter tidak ditemukan dengan ID: " + request.getDoctorId()));

        HospitalService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Layanan tidak ditemukan dengan ID: " + request.getServiceId()));

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .service(service)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPatients(request.getMaxPatients())
                .bookedCount(0)
                .isDeleted(false)
                .build();
        DoctorSchedule savedSchedule = scheduleRepository.save(schedule);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Jadwal dokter berhasil dibuat.");
        response.put("scheduleId", savedSchedule.getId());
        response.put("dayOfWeek", savedSchedule.getDayOfWeek());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
