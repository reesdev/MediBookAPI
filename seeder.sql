USE medibook_db;

-- insert user login (password default: 'password' terenkripsi bcrypt)
INSERT INTO users (id, username, password, email, role, is_deleted) VALUES
(1, 'reesky_admin', '$2a$10$4N/uTpZ.IwIfPOuFyYvgaeYbkJy0DE7PW7i7yGEA7s2bdFowEvB/K', 'reesky.it@gmail.com', 'ADMIN', 0),
(2, 'dr_hendrawan', '$2a$10$4N/uTpZ.IwIfPOuFyYvgaeYbkJy0DE7PW7i7yGEA7s2bdFowEvB/K', 'dr.hendrawan.spa@gmail.com', 'DOCTOR', 0),
(3, 'dr_siti_rahmah', '$2a$10$4N/uTpZ.IwIfPOuFyYvgaeYbkJy0DE7PW7i7yGEA7s2bdFowEvB/K', 'sitirahmah.pd@yahoo.co.id', 'DOCTOR', 0),
(4, 'budi_santoso', '$2a$10$4N/uTpZ.IwIfPOuFyYvgaeYbkJy0DE7PW7i7yGEA7s2bdFowEvB/K', 'budi.santoso90@gmail.com', 'PATIENT', 0),
(5, 'ani_wijaya', '$2a$10$4N/uTpZ.IwIfPOuFyYvgaeYbkJy0DE7PW7i7yGEA7s2bdFowEvB/K', 'ani.wijaya@outlook.com', 'PATIENT', 0);

-- insert profil dokter
INSERT INTO doctors (id, user_id, full_name, specialization, sip, phone, email, is_deleted) VALUES
(1, 2, 'dr. Hendrawan Wijaya, Sp.A', 'Spesialis Anak', '446/2083/SIP-D/411/2024', '081234567890', 'dr.hendrawan.spa@gmail.com', 0),
(2, 3, 'dr. Siti Rahmah, Sp.PD', 'Spesialis Penyakit Dalam', '446/1042/SIP-D/411/2023', '085698765432', 'sitirahmah.pd@yahoo.co.id', 0);

-- insert data pasien
INSERT INTO patients (id, user_id, full_name, nik, phone, birth_date, gender, address, is_deleted) VALUES
(1, 4, 'Budi Santoso', '3201020304050001', '081398761234', '1990-05-15', 'Laki-laki', 'Jl. Nginden Semolo No. 38, Sukolilo, Surabaya', 0),
(2, 5, 'Ani Wijaya', '3201020304050002', '082232433695', '1995-10-20', 'Perempuan', 'Jl. Raya Bromo No. 124, Triwung Kidul, Probolinggo', 0);

-- insert master layanan rumah sakit
INSERT INTO hospital_services (id, name, category, description, base_price, is_deleted) VALUES
(1, 'Konsultasi Poli Anak', 'POLIKLINIK', 'Pemeriksaan kesehatan bayi dan anak sakit maupun konsultasi tumbuh kembang', 150000.00, 0),
(2, 'Konsultasi Poli Penyakit Dalam', 'POLIKLINIK', 'Pemeriksaan kesehatan organ dalam, diabetes, hipertensi, dll', 180000.00, 0),
(3, 'Cek Darah Lengkap + Kolesterol', 'LAB', 'Pemeriksaan laboratorium sel darah lengkap, kolesterol total, asam urat dan gula darah', 220000.00, 0),
(4, 'Rontgen Thorax AP', 'RAD', 'Pemeriksaan radiologi foto dada/paru-paru', 250000.00, 0);

-- insert jadwal praktek dokter
INSERT INTO doctor_schedules (id, doctor_id, service_id, day_of_week, start_time, end_time, max_patients, booked_count, is_deleted) VALUES
(1, 1, 1, 1, '09:00:00', '12:00:00', 10, 0, 0), -- dr. Hendrawan - Poli Anak (Senin)
(2, 1, 1, 3, '14:00:00', '17:00:00', 10, 0, 0), -- dr. Hendrawan - Poli Anak (Rabu)
(3, 2, 2, 2, '10:00:00', '13:00:00', 15, 0, 0); -- dr. Siti Rahmah - Poli Penyakit Dalam (Selasa)
