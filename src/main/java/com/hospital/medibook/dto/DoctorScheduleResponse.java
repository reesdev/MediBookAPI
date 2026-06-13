package com.hospital.medibook.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private DoctorResponse doctor;
    private HospitalServiceResponse service;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private Integer bookedCount;
    private Integer availableQuota;
}
