# MediBook API - Hospital Booking & Management System

MediBook API adalah sistem backend terintegrasi untuk manajemen pelayanan dan pendaftaran (booking) pasien di rumah sakit secara mandiri. Sistem ini dirancang untuk memproses pendaftaran pasien rawat jalan pada poliklinik spesialis maupun layanan penunjang medis (seperti cek lab darah dan rontgen radiologi) secara langsung tanpa dokter.



---

## 1. Arsitektur & Teknologi Utama

- **Bahasa Pemrograman**: Java 17
- **Web Framework**: Java Spring Boot v3.2.5
- **ORM / Data Access**: Hibernate (menggunakan Spring Data JPA)
- **Database**: MySQL (Production/Staging) & H2 (In-Memory untuk unit testing)
- **Database Migration**: **Liquibase Migration (YAML)** (skrip migrasi ditaruh di `src/main/resources/db/changelog/`)
- **Otentikasi & Keamanan**: JWT (JSON Web Token) stateless dengan Spring Security 6.x
- **Dokumentasi API**: Swagger UI (Springdoc OpenAPI v2.5.0)
- **Caching Layer**: Redis Cache (untuk data katalog dan token idempotency)

---

## 2. Fitur Utama (Core Features)

1. **Stateless Security**: Autentikasi berbasis JWT dengan Role-Based Access Control (RBAC) yang memisahkan hak akses Pasien, Dokter, dan Admin.
2. **Safe Concurrency**: Pendaftaran booking diproteksi dari masalah over-limit kuota menggunakan Database Locking dan pencatatan parameter `booked_count` langsung pada tabel jadwal dokter.
3. **Task Scheduler**: Pembersihan otomatis data booking yang tidak dibayar dalam waktu 15 menit untuk mengembalikan kuota ke sistem.
4. **Compliance Ready & Auditability**: Implementasi Soft Delete (`is_deleted` flag) di semua tabel master, log audit status medis (`booking_events`), serta pencatatan terpisah untuk data transaksi finansial (`transactions`).
5. **Quality of Service (Rating & Review)**: Pasien dapat memberikan rating bintang (1-5) dan ulasan pelayanan dokter/layanan rumah sakit setelah status booking diselesaikan.

---

## 3. Alur Bisnis Utama (Business Flow)

Berikut adalah alur operasional utama MediBook API yang dirancang secara linear:

1. **Pencarian & Pemilihan Layanan**: Pasien mencari layanan poliklinik/dokter spesialis atau penunjang medis (Lab/Radiologi) yang aktif.
2. **Reservasi & Antrean (Safe Booking)**:
   - Pasien mengirim data pendaftaran.
   - Sistem memvalidasi ketersediaan slot kapasitas dokter dengan melakukan *Pessimistic Lock* pada data jadwal.
   - Jika kuota tersedia, sistem menambah jumlah `booked_count` di jadwal, membuat nomor antrean urut harian, dan menerbitkan kode booking dengan status awal `PENDING_PAYMENT`.
3. **Pembayaran & Log Transaksi**: Pasien melakukan pembayaran sandbox. Sistem mengubah status booking ke `CONFIRMED` dan mencatat data transaksi finansial ke tabel log `transactions`.
4. **Pembersihan Otomatis (Auto-Cancel)**: Jika pembayaran tidak diselesaikan dalam 15 menit, Scheduler background task otomatis membatalkan booking (`CANCELLED`), mengurangi nilai `booked_count` jadwal, dan memperbarui status transaksi.
5. **Pelayanan Medis & Ulasan**: Dokter memproses pasien (`IN_PROGRESS` -> `COMPLETED`). Setelah selesai, pasien dapat memberikan rating dan ulasan pelayanan pada tabel `reviews`.

---

## 4. Struktur Layer & Migrasi Database (Spring Boot)

Pemisahan tanggung jawab kode backend diatur rapi dalam struktur paket Spring Boot berikut:

* **`db/changelog` (Liquibase Database Migration)**:
  Mengelola perubahan skema database secara terstruktur versi demi versi menggunakan format YAML. File changelog utama berada di `src/main/resources/db/changelog/db.changelog-master.yaml` yang mengimpor seluruh file migrasi di folder `migration/` dengan format penamaan `V{nomor}__{deskripsi}.yaml` (misalnya `V001__create_users_table.yaml`).
* **`config` (Configuration Layer)**:
  Menyimpan konfigurasi filter keamanan (Spring Security), konfigurasi CORS, setup koneksi Redis, dan dokumentasi API (Swagger/OpenAPI).
* **`controller` (API Controller Layer)**:
  Sebagai pintu gerbang REST API. Menerima HTTP Request, validasi format input `@Valid`, dan mengembalikan data response terstruktur (DTO).
* **`service` (Business Logic Layer)**:
  Pusat logika bisnis rumah sakit (validasi kuota, kalkulasi nomor antrean, proses pembayaran, scheduler, dan rating).
* **`repository` (Data Access Layer)**:
  Berkomunikasi dengan MySQL menggunakan Spring Data JPA. Menyediakan query transaksi khusus seperti *Pessimistic Locking* (`SELECT FOR UPDATE`) pada tabel `doctor_schedules`.
* **`entity` (JPA Entity Model)**:
  Representasi fisik tabel database MySQL dengan dukungan kolom status `is_deleted` untuk mendukung **Soft Delete** di tingkat database.
* **`dto` (Data Transfer Object)**:
  Struktur objek data untuk Request (input dari user) dan Response (output kiriman ke user) untuk memisahkan data sensitif dari entity.
* **`exception` (Error Handler Layer)**:
  Menangani penanganan error sistem secara terpusat (Global Exception Handler) untuk mengembalikan status HTTP dan pesan kesalahan yang rapi.

---

## 5. Desain Database & ERD

### A. Kamus Data (Logical Schema)
1. **`users`**: Akun login (id, username, password, email, role (ENUM), is_deleted, created_at).
2. **`patients`**: Profil rekam identitas pasien (id, user_id (FK RESTRICT), full_name, nik, phone, birth_date, gender (ENUM), address, is_deleted).
3. **`doctors`**: Profil spesialisasi dokter (id, user_id (FK RESTRICT), full_name, specialization, sip, phone, email, is_deleted).
4. **`hospital_services`**: Jenis pelayanan pemeriksaan medis (id, name, category (ENUM), description, base_price, is_deleted).
5. **`doctor_schedules`**: Jadwal hari kerja praktek dokter (id, doctor_id (FK RESTRICT), service_id (FK RESTRICT), day_of_week, start_time, end_time, max_patients, **booked_count**, is_deleted).
6. **`bookings`**: Transaksi pendaftaran pasien (id, booking_code, patient_id (FK RESTRICT), service_id (FK RESTRICT), doctor_id (FK RESTRICT), schedule_id (FK RESTRICT), booking_date, queue_number, status (ENUM), complaint, total_fee, created_at, updated_at).
7. **`transactions`**: Log transaksi pembayaran finansial (id, booking_id (FK RESTRICT), transaction_code, amount, payment_method, status (ENUM), paid_at, created_at).
8. **`booking_events`**: Jejak audit status pendaftaran (id, booking_id (FK RESTRICT), status, event_type, actor (ENUM), detail, created_at).
9. **`medical_documents`**: Metadata berkas rujukan/resep digital (id, booking_id (FK RESTRICT), file_path, original_file_name, file_size, content_type, uploaded_by (ENUM), document_type (ENUM), created_at).
10. **`reviews`**: Rating & ulasan pelayanan pasien (id, booking_id (FK RESTRICT, Unique), patient_id (FK RESTRICT), doctor_id (FK RESTRICT, Nullable), service_id (FK RESTRICT), rating, review_text, created_at).

> [!IMPORTANT]
> **Aturan Cascade & Integritas Relasional**: Seluruh foreign key menggunakan aturan `ON DELETE RESTRICT` (kecuali audit logs yang bisa menyesuaikan, namun demi keamanan semua diatur ke RESTRICT). Jika data induk (seperti pasien, dokter, atau jadwal) dihapus secara fisik, database akan menolaknya jika masih memiliki relasi transaksi.

### B. Indeks Database
Tabel `bookings` dilengkapi indeks khusus untuk mempercepat pencarian data dengan trafik tinggi:
* Index pada `booking_code` untuk mempercepat query detail booking.
* Index pada `patient_id` & `doctor_id` untuk pencarian riwayat pasien/dokter.
* Index composite pada `(status, created_at)` untuk mengoptimalkan pencarian background scheduler.

### C. Diagram Hubungan Entitas (ERD)
Database dirancang menggunakan skema relasi berikut (merujuk pada berkas [ERD FINAL PROJECT.drawio.png](ERD%20FINAL%20PROJECT.drawio.png)):

![ERD Diagram](ERD%20FINAL%20PROJECT.drawio.png)

---

## 6. Matrix Use Case & Peran Pengguna (RBAC)

Berikut adalah diagram use case peran pengguna di dalam sistem (merujuk pada berkas [USE CASE RBAC.png](USE%20CASE%20RBAC.png)):

![Use Case Diagram](USE%20CASE%20RBAC.png)

### Tabel Matriks Use Case
Berikut adalah detail pemetaan fungsi fitur untuk setiap peran pengguna:

| Fitur / Use Case | Pasien (PATIENT) | Dokter (DOCTOR) | Admin (ADMIN) | Jenis Otorisasi |
| :--- | :---: | :---: | :---: | :--- |
| Registrasi & Login | Ya | Ya (Daftar manual) | Tidak (Seed Awal) | Publik / Anonymous |
| Melihat Katalog Dokter & Layanan | Ya | Ya | Ya | Bearer JWT (Semua Peran) |
| Membuat Reservasi Booking | Ya | Tidak | Tidak | Bearer JWT (Role: PATIENT) |
| Simulasi Sandbox Pembayaran | Ya | Tidak | Tidak | Bearer JWT (Role: PATIENT) |
| Menginput Rating & Ulasan | Ya | Tidak | Tidak | Bearer JWT (Role: PATIENT) |
| Melihat Antrean Hari Ini | Tidak | Ya | Ya | Bearer JWT (Role: DOCTOR, ADMIN) |
| Mengubah Status Medis Pasien | Tidak | Ya | Tidak | Bearer JWT (Role: DOCTOR) |
| CRUD Master Data (Dokter, Layanan, Jadwal) | Tidak | Tidak | Ya | Bearer JWT (Role: ADMIN) |
| Melihat Laporan Keuangan & Statistik | Tidak | Tidak | Ya | Bearer JWT (Role: ADMIN) |

---

## 7. Dokumentasi REST API Contract

Berikut adalah rancangan detail RESTful API beserta format request dan respon JSON:

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
  "address": "Jl. Nginden Semolo No. 38, Sukolilo, Surabaya"
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

### B. Login Akun (JWT Token)
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

### C. Mencari Daftar Dokter Terpaginasi
* **Endpoint**: `GET /api/doctors`
* **Query Parameters**:
  - `page`: 0 (Default)
  - `size`: 10 (Default)
  - `specialization`: "Spesialis Anak" (Opsional)
  - `search`: "Hendra" (Opsional)
* **Response Sukses (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "fullName": "dr. Hendrawan Wijaya, Sp.A",
      "specialization": "Spesialis Anak",
      "sip": "446/2083/SIP-D/411/2024",
      "phone": "081234567890",
      "email": "dr.hendrawan.spa@gmail.com"
    }
  ],
  "currentPage": 0,
  "totalPages": 1,
  "totalItems": 1
}
```

### D. Membuat Booking Baru
* **Endpoint**: `POST /api/bookings`
* **Authorization**: `Bearer <JWT_TOKEN>` (Role: `PATIENT`)
* **Headers**: `Content-Type: multipart/form-data`
* **Request Form-Data**:
  - `serviceId`: 1
  - `doctorId`: 1
  - `scheduleId`: 1
  - `bookingDate`: "2026-06-08"
  - `complaint`: "Anak demam tinggi"
  - `referralFile`: [File rujukan medis PDF - opsional]
* **Response Sukses (201 Created)**:
```json
{
  "bookingCode": "BK-20260608-0001",
  "queueNumber": 1,
  "bookingDate": "2026-06-08",
  "status": "PENDING_PAYMENT",
  "totalFee": 150000.00,
  "message": "Booking created successfully. Please complete payment within 15 minutes."
}
```
* **Response Error - Kuota Penuh (400 Bad Request)**:
```json
{
  "timestamp": "2026-06-06T13:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Kuota dokter untuk jadwal yang dipilih sudah penuh."
}
```

### E. Simulasi Pembayaran Sandbox
* **Endpoint**: `POST /api/bookings/{id}/pay`
* **Authorization**: `Bearer <JWT_TOKEN>` (Role: `PATIENT`)
* **Request Body (JSON)**:
```json
{
  "paymentMethod": "BCA Virtual Account",
  "amount": 150000.00
}
```
* **Response Sukses (200 OK)**:
```json
{
  "bookingCode": "BK-20260608-0001",
  "status": "CONFIRMED",
  "paymentTime": "2026-06-06T13:42:00Z",
  "transactionCode": "TX-20260606-88910",
  "message": "Payment simulation successful. Appointment is confirmed."
}
```

### F. Memberikan Ulasan Pelayanan
* **Endpoint**: `POST /api/bookings/{id}/reviews`
* **Authorization**: `Bearer <JWT_TOKEN>` (Role: `PATIENT`)
* **Request Body (JSON)**:
```json
{
  "rating": 5,
  "reviewText": "Dokternya sangat sabar dan penjelasannya mudah dipahami."
}
```
* **Response Sukses (210 Created)**:
```json
{
  "reviewId": 1,
  "bookingCode": "BK-20260608-0001",
  "rating": 5,
  "message": "Review submitted successfully. Thank you for your feedback."
}
```

---

## 8. Penyiapan Database Relasional Lokal

Langkah-langkah untuk menjalankan database relasional MySQL secara lokal:
1. Buat database baru bernama `medibook_db` di server MySQL Anda.
2. Jalankan server Redis lokal Anda (port default: 6379).
3. Jalankan aplikasi Spring Boot. Liquibase akan mendeteksi dan mengeksekusi seluruh skema tabel (`V001` s.d `V010`) dan data awal (`V011`) secara otomatis.
