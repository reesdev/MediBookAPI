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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
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
