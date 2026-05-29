# MediBook API - Hospital Booking & Management System

MediBook API adalah sistem backend terintegrasi untuk manajemen pelayanan dan pendaftaran (booking) pasien di rumah sakit secara mandiri. Sistem ini dirancang untuk memproses pendaftaran pasien rawat jalan pada poliklinik spesialis maupun layanan penunjang medis (seperti cek lab darah dan rontgen radiologi) secara langsung tanpa dokter.

Repositori ini disiapkan khusus sebagai **Dokumen Perancangan PRD (Product Requirements Document) & Tata Kelola Database** untuk memenuhi penilaian **Check Point 1 (PPT & Database Management)**.

---

## 1. Arsitektur & Teknologi Utama

- **Bahasa Pemrograman**: Java 17
- **Web Framework**: Java Spring Boot v3.2.5
- **ORM / Data Access**: Hibernate (menggunakan Spring Data JPA)
- **Database**: MySQL (Production/Staging) & H2 (In-Memory untuk unit testing)
- **Otentikasi & Keamanan**: JWT (JSON Web Token) stateless dengan Spring Security 6.x
- **Dokumentasi API**: Swagger UI (Springdoc OpenAPI v2.5.0)
- **Caching Layer**: Redis Cache (untuk optimasi data katalog dan token idempotency)

---

## 2. Fitur Utama (Core Features)

1. **Stateless Security**: Autentikasi berbasis JWT dengan Role-Based Access Control (RBAC) yang memisahkan hak akses Pasien, Dokter, dan Admin.
2. **Safe Concurrency**: Mekanisme booking yang diproteksi dari masalah over-limit kuota menggunakan Database Locking.
3. **Task Scheduler**: Pembersihan otomatis data booking yang tidak dibayar dalam waktu 15 menit untuk mengembalikan kuota ke sistem.
4. **Compliance Ready**: Implementasi Soft Delete untuk menyembunyikan rekam medis non-aktif tanpa melanggar hukum retensi data historis pasien.

---

## 3. Alur Bisnis Utama (Business Flow)

Sesuai dengan alur operasional pada slide presentasi Check Point 1, alur pemesanan dan transaksi pada MediBook API dirancang secara sederhana sebagai berikut:

1. **Pencarian & Pemilihan Layanan**: Pasien mencari layanan poliklinik/dokter spesialis atau penunjang medis (Lab/Radiologi) yang aktif.
2. **Reservasi & Antrean (Safe Booking)**:
   - Pasien mengirim data pendaftaran.
   - Sistem memvalidasi ketersediaan slot kapasitas dokter secara aman (*concurrency safe*).
   - Jika kuota tersedia, sistem membuat nomor antrean urut harian dan menerbitkan kode booking dengan status awal `PENDING_PAYMENT`.
3. **Konfirmasi Pembayaran**: Pasien melakukan simulasi pembayaran melalui endpoint sandbox. Status booking terupdate menjadi `CONFIRMED`.
4. **Pembersihan Otomatis (Auto-Cancel)**: Jika pembayaran tidak terkonfirmasi dalam batas waktu 15 menit, Scheduler background task otomatis membatalkan booking (`CANCELLED`) dan mengembalikan kuota slot ke sistem.

---

## 4. Struktur Layer Aplikasi (Spring Boot Layers)

Pemisahan tanggung jawab kode backend diatur rapi dalam struktur paket Spring Boot berikut:

* **`config` (Configuration Layer)**:
  Menyimpan konfigurasi filter keamanan (Spring Security), konfigurasi CORS, setup koneksi Redis, dan dokumentasi API (Swagger/OpenAPI).
* **`controller` (API Controller Layer)**:
  Sebagai pintu gerbang masuk REST API. Bertanggung jawab menerima HTTP Request dari client, memvalidasi format input dasar, serta mengembalikan response data terstruktur (DTO).
* **`service` (Business Logic Layer)**:
  Pusat seluruh aturan dan logika bisnis rumah sakit. Di layer ini dilakukan pengecekan sisa kuota, kalkulasi nomor antrean otomatis, proses simulasi pembayaran sandbox, dan pembersihan terjadwal pendaftaran kadaluwarsa (*scheduler task*).
* **`repository` (Data Access Layer)**:
  Berkomunikasi langsung dengan database MySQL menggunakan Spring Data JPA. Menyediakan query transaksi khusus seperti *Pessimistic Locking* (`SELECT FOR UPDATE`) untuk validasi kuota secara berurutan.
* **`entity` (JPA Entity Model)**:
  Representasi fisik dari tabel database MySQL. Memetakan relasi data model, tipe data kolom, dan penampung flag `is_deleted` untuk kebutuhan *Soft Delete*.
* **`dto` (Data Transfer Object)**:
  Struktur objek terpisah untuk Request (input data dari user) dan Response (output kiriman ke user). Berfungsi menyembunyikan data sensitif dari eksposur langsung tabel database.
* **`exception` (Error Handler Layer)**:
  Menangani penanganan error sistem secara terpusat (Global Exception Handler) untuk mengembalikan status HTTP dan pesan kesalahan yang rapi ke sisi client jika terjadi kegagalan.
* **`validation` (Custom Validator)**:
  Menyediakan anotasi validasi kustom (seperti format penulisan tanggal kunjungan dan pencegahan nominal negatif).
* **`util` (Utility Layer)**:
  Menyimpan fungsi pembantu umum (seperti generator token JWT, parsing claims, dan enkripsi penamaan berkas fisik di server).

---

## 5. Desain Database & ERD

### A. Kamus Data (Logical Schema)
1. **`users`**: Akun login (id, username, password, email, role, is_deleted, created_at).
2. **`patients`**: Profil rekam identitas pasien (id, user_id, full_name, nik, phone, birth_date, gender, address, is_deleted).
3. **`doctors`**: Profil spesialisasi dokter (id, user_id, full_name, specialization, sip, phone, email, is_deleted).
4. **`hospital_services`**: Jenis pelayanan pemeriksaan medis (id, name, category, description, base_price, is_deleted).
5. **`doctor_schedules`**: Jadwal hari kerja praktek dokter (id, doctor_id, service_id, day_of_week, start_time, end_time, max_patients, is_deleted).
6. **`bookings`**: Transaksi pendaftaran pasien (id, booking_code, patient_id, service_id, doctor_id, schedule_id, booking_date, queue_number, status, complaint, total_fee, created_at, updated_at).
7. **`booking_events`**: Jejak audit status pendaftaran (id, booking_id, status, event_type, actor, detail, created_at).
8. **`medical_documents`**: Metadata berkas rujukan/resep digital (id, booking_id, file_path, original_file_name, file_size, content_type, uploaded_by, document_type, created_at).

### B. Diagram Hubungan Entitas (ERD)
Database dirancang menggunakan skema relasi berikut (merujuk pada berkas [ERD FINAL PROJECT.drawio.png](ERD%20FINAL%20PROJECT.drawio.png)):

![ERD Diagram](ERD%20FINAL%20PROJECT.drawio.png)

---

## 6. Dokumentasi REST API Contract

Berikut adalah kontrak rancangan RESTful API lengkap dengan format payload input dan kembalian response:

### A. Registrasi Akun Pasien
* **Endpoint**: `POST /api/auth/register`
* **Request Body (JSON)**:
```json
{
  "username": "pasien_budi",
  "password": "mySecurePassword123",
  "email": "budi.santoso@gmail.com",
  "fullName": "Budi Santoso",
  "nik": "3201020304050001",
  "phone": "085511223344",
  "birthDate": "1990-05-15",
  "gender": "Laki-laki",
  "address": "Jl. Merdeka No. 12, Jakarta"
}
```
* **Response Sukses (201 Created)**:
```json
{
  "message": "User registered successfully",
  "userId": 4,
  "username": "pasien_budi",
  "role": "PATIENT"
}
```

### B. Login Akun (Mendapatkan Token JWT)
* **Endpoint**: `POST /api/auth/login`
* **Request Body (JSON)**:
```json
{
  "username": "pasien_budi",
  "password": "mySecurePassword123"
}
```
* **Response Sukses (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXNpZW5fYnVkaSIsInJvbGUiOiJQQVRJRU5UIn0...",
  "role": "PATIENT",
  "username": "pasien_budi"
}
```

### C. Membuat Transaksi Booking Medis
* **Endpoint**: `POST /api/bookings`
* **Authorization**: `Bearer <JWT_TOKEN>` (Role: `PATIENT`)
* **Headers**: `Content-Type: multipart/form-data`
* **Request Form-Data**:
  - `serviceId`: 1
  - `doctorId`: 1
  - `scheduleId`: 1
  - `bookingDate`: "2026-06-01"
  - `complaint`: "Keluhan medis pasien"
  - `referralFile`: [File PDF / PNG / JPG rujukan medis - opsional]
* **Response Sukses (201 Created)**:
```json
{
  "bookingCode": "BK-20260601-0001",
  "queueNumber": 1,
  "bookingDate": "2026-06-01",
  "status": "PENDING_PAYMENT",
  "totalFee": 150000.00,
  "message": "Booking created successfully. Please complete payment within 15 minutes."
}
```

### D. Simulasi Pembayaran Sandbox
* **Endpoint**: `POST /api/bookings/{id}/pay`
* **Authorization**: `Bearer <JWT_TOKEN>` (Role: `PATIENT`)
* **Response Sukses (200 OK)**:
```json
{
  "bookingCode": "BK-20260601-0001",
  "status": "CONFIRMED",
  "paymentTime": "2026-05-30T02:40:15Z",
  "message": "Payment simulation successful. Appointment is confirmed."
}
```

---

## 7. Penyiapan Database Relasional Lokal

Langkah-langkah untuk menjalankan database relasional MySQL secara lokal:
1. Buat skema tabel dengan mengeksekusi script DDL migrasi dari file **[schema.sql](schema.sql)**.
2. Isi data pengujian awal dengan mengeksekusi script pengisi data dummy dari file **[seeder.sql](seeder.sql)**.
