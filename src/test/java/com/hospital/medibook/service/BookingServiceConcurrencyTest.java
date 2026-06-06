package com.hospital.medibook.service;

import com.hospital.medibook.constant.*;
import com.hospital.medibook.dto.BookingRequest;
import com.hospital.medibook.dto.BookingResponse;
import com.hospital.medibook.entity.*;
import com.hospital.medibook.exception.BadRequestException;
import com.hospital.medibook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class BookingServiceConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalServiceRepository serviceRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private DoctorSchedule savedSchedule;
    private List<User> testUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Mocking Redis Idempotency to always allow locking
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn(true);

        // Bersihkan data jika ada sisa dari test lain
        scheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        serviceRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Buat User Dokter
        User docUser = User.builder()
                .username("test_doctor_user")
                .password("password")
                .email("doctor@hospital.com")
                .role(Role.DOCTOR)
                .isDeleted(false)
                .build();
        userRepository.save(docUser);

        // 2. Buat Profil Dokter
        Doctor doctor = Doctor.builder()
                .user(docUser)
                .fullName("dr. Test Concurrency")
                .specialization("Spesialis Lock")
                .sip("SIP-12345")
                .phone("081234")
                .email("doctor@hospital.com")
                .isDeleted(false)
                .build();
        doctorRepository.save(doctor);

        // 3. Buat Layanan
        HospitalService service = HospitalService.builder()
                .name("Layanan Tes Concurrency")
                .category(ServiceCategory.POLIKLINIK)
                .description("Tes")
                .basePrice(new BigDecimal("100000.00"))
                .isDeleted(false)
                .build();
        serviceRepository.save(service);

        // 4. Buat Jadwal Praktek (Kapasitas Maksimal = 2)
        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .service(service)
                .dayOfWeek(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .maxPatients(2)
                .bookedCount(0)
                .isDeleted(false)
                .build();
        savedSchedule = scheduleRepository.save(schedule);

        // 5. Buat 5 User & Profil Pasien untuk pendaftaran bersamaan
        testUsers.clear();
        for (int i = 1; i <= 5; i++) {
            User pUser = User.builder()
                    .username("patient_user_" + i)
                    .password("password")
                    .email("patient_" + i + "@mail.com")
                    .role(Role.PATIENT)
                    .isDeleted(false)
                    .build();
            userRepository.save(pUser);
            testUsers.add(pUser);

            Patient patient = Patient.builder()
                    .user(pUser)
                    .fullName("Patient Name " + i)
                    .nik("123456789012340" + i)
                    .phone("0855" + i)
                    .birthDate(LocalDate.of(1990, 1, i))
                    .gender(Gender.LAKI_LAKI)
                    .address("Alamat " + i)
                    .isDeleted(false)
                    .build();
            patientRepository.save(patient);
        }
    }

    @Test
    public void testConcurrentBookingsQuotaLimit() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String username = testUsers.get(i).getUsername();
            executorService.submit(() -> {
                try {
                    // Tunggu sinyal mulai bersamaan
                    latch.await();

                    // Set security context per thread
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))
                            )
                    );

                    BookingRequest request = BookingRequest.builder()
                            .doctorId(savedSchedule.getDoctor().getId())
                            .serviceId(savedSchedule.getService().getId())
                            .scheduleId(savedSchedule.getId())
                            .bookingDate(LocalDate.now().plusDays(1))
                            .complaint("Tes Concurrency")
                            .build();

                    bookingService.createBooking(request);
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    if (e.getMessage().contains("Kuota dokter untuk jadwal yang dipilih sudah penuh")) {
                        failureCount.incrementAndGet();
                    } else {
                        System.err.println("Pesan error tidak terduga: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Terjadi exception lain: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Lepaskan gerbang agar semua thread mulai mengeksekusi secara bersamaan
        latch.countDown();

        // Tunggu semua thread selesai
        finishLatch.await(5, TimeUnit.SECONDS);

        // Verifikasi hasil:
        // Kuota = 2, pendaftar = 5.
        // Maka harus ada tepat 2 sukses dan 3 gagal kuota penuh.
        assertEquals(2, successCount.get(), "Pendaftaran yang sukses harus tepat berjumlah 2");
        assertEquals(3, failureCount.get(), "Pendaftaran yang gagal (kuota penuh) harus tepat berjumlah 3");

        // Cek bookedCount di database
        DoctorSchedule updatedSchedule = scheduleRepository.findById(savedSchedule.getId()).orElseThrow();
        assertEquals(2, updatedSchedule.getBookedCount(), "bookedCount di database harus bernilai 2");

        executorService.shutdown();
    }
}
