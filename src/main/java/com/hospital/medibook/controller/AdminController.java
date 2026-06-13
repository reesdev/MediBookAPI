package com.hospital.medibook.controller;

import com.hospital.medibook.dto.DoctorCreateRequest;
import com.hospital.medibook.dto.ScheduleCreateRequest;
import com.hospital.medibook.dto.ServiceCreateRequest;
import com.hospital.medibook.dto.DoctorResponse;
import com.hospital.medibook.dto.DoctorScheduleResponse;
import com.hospital.medibook.dto.HospitalServiceResponse;
import com.hospital.medibook.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "4. Admin API", description = "Endpoint khusus untuk manajemen Rumah Sakit")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/doctors")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        DoctorResponse response = adminService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/services")
    public ResponseEntity<HospitalServiceResponse> createService(@Valid @RequestBody ServiceCreateRequest request) {
        HospitalServiceResponse response = adminService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/schedules")
    public ResponseEntity<DoctorScheduleResponse> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        DoctorScheduleResponse response = adminService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
