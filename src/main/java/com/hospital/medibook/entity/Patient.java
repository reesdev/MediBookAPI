package com.hospital.medibook.entity;

import com.hospital.medibook.constant.Gender;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_patients_user"))
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 20)
    private String nik;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, columnDefinition = "ENUM('Laki-laki', 'Perempuan')")
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default private boolean isDeleted = false;
}
