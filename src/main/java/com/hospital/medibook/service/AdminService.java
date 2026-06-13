package com.hospital.medibook.service;

import com.hospital.medibook.constant.Role;
import com.hospital.medibook.constant.ServiceCategory;
import com.hospital.medibook.dto.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final HospitalServiceRepository serviceRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public DoctorResponse createDoctor(DoctorCreateRequest request) {
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

        return DoctorResponse.builder()
                .id(savedDoctor.getId())
                .fullName(savedDoctor.getFullName())
                .specialization(savedDoctor.getSpecialization())
                .sip(savedDoctor.getSip())
                .phone(savedDoctor.getPhone())
                .email(savedDoctor.getEmail())
                .build();
    }

    @Transactional
    public HospitalServiceResponse createService(ServiceCreateRequest request) {
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

        return HospitalServiceResponse.builder()
                .id(savedService.getId())
                .name(savedService.getName())
                .category(savedService.getCategory().name())
                .description(savedService.getDescription())
                .basePrice(savedService.getBasePrice())
                .build();
    }

    @Transactional
    public DoctorScheduleResponse createSchedule(ScheduleCreateRequest request) {
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

        return DoctorScheduleResponse.builder()
                .id(savedSchedule.getId())
                .doctor(DoctorResponse.builder().id(savedSchedule.getDoctor().getId()).fullName(savedSchedule.getDoctor().getFullName()).build())
                .service(HospitalServiceResponse.builder().id(savedSchedule.getService().getId()).name(savedSchedule.getService().getName()).build())
                .dayOfWeek(savedSchedule.getDayOfWeek())
                .startTime(savedSchedule.getStartTime())
                .endTime(savedSchedule.getEndTime())
                .maxPatients(savedSchedule.getMaxPatients())
                .bookedCount(savedSchedule.getBookedCount())
                .availableQuota(savedSchedule.getMaxPatients()) // bookedCount is 0 initially
                .build();
    }
}
