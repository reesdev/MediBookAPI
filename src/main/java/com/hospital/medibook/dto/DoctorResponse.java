package com.hospital.medibook.dto;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String fullName;
    private String specialization;
    private String sip;
    private String phone;
    private String email;
}
