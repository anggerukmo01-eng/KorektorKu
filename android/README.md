# KorektorKu — Android MVP v5.0 (penguatan logika & pemindaian)

Aplikasi Android **offline-first** untuk penilaian siswa berbasis PG, isian, uraian, dan ekspor Excel. Ini **source project**, bukan APK terkompilasi. UI Android menggunakan komponen platform standar untuk mengurangi dependensi.

## Kesiapan fitur

| Fitur | Implementasi v5 | Catatan |
|---|---|---|
| Input jumlah soal PG (0-50), isian (0-20), uraian (0-10) | Ada | Minimal total 1 |
| Kunci PG A-E & isian multi-alternatif `|` | Ada | Input guru; validasi sebelum simpan |
| Bobot per jenis atau per nomor soal | Ada | Default bobot jenis + opsi bobot khusus tiap nomor |
| POIN, PROPORSI, FORMULA_MANUAL, CUSTOM_EXPR | Ada | Parser aman tanpa `eval` |
| Skor uraian dan parsial isian | Uraian: ada. Isian parsial: engine saja | UI isian parsial belum tersedia |
| Review jawaban ragu/ganda/blank | Ada | '?' ragu; '-' blank; blank input belum diproses |
| Verifikasi final & perubahan versi kunci/formula | Ada | Perubahan versi menahan nilai lama |
| Ekspor `.xlsx` 6 sheet | Ada | Snapshot nilai, bukan workbook dengan rumus editable |
| Generate LJK PDF 4 marker (A-E) | Ada | Khusus PG 1–50 |
| Scanner foto / galeri + deteksi gelembung | Eksperimental | Hanya template PG ini; wajib guru validasi, belum tes lapangan |
| OCR tulisan tangan isian/uraian | Belum | Input manual hingga fase berikutnya |
| Rubrik uraian multi-kriteria | Belum | Skor akhir uraian dimasukkan guru |
| Room, login/PIN, enkripsi database, audit log | Belum | SharedPreferences privat; perlu migrasi sebelum produksi sekolah |
| Build APK / uji perangkat Android | Belum diuji di lingkungan ini | Memerlukan Android SDK + Gradle |

## Membuka sebagai proyek Android

1. Ekstrak ZIP proyek ke satu folder bernama `KorektorKu_Android_MVP_V5`.
2. Buka **Android Studio** → **Open** → pilih folder tersebut (yang berisi `settings.gradle.kts`).
3. Konfigurasikan **JDK 17** (Gradle JDK), **Gradle 8.9**, **Android SDK 35 / Build Tools 35.0.0**. Dependensi plugin dipatok AGP `8.7.3` dan Kotlin `2.0.21` (mengunduh dari repositori Google/Maven ketika pertama sync). Tidak ada berkas Gradle wrapper dalam arsip; pilih *local Gradle 8.9* atau buat wrapper dengan Gradle terpasang: `gradle wrapper --gradle-version 8.9`. Workflow GitHub Actions sudah mengatur Gradle 8.9 secara otomatis.
4. Setelah Gradle sync dan tanpa error: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**. Atau terminal: `gradle :app:assembleDebug`.
5. Hasil lazim: `app/build/outputs/apk/debug/app-debug.apk`; pindahkan ke HP dan instal. Jika build gagal, lampirkan log, bukan menganggap APK sudah valid.
6. Sebelum penggunaan nyata, uji manual foto LJK dan ekspor Excel menggunakan perangkat target.

**Alternatif tanpa SDK lokal:** upload isi folder ke repositori GitHub pada root proyek; workflow `.github/workflows/android-debug.yml` membangun APK di GitHub Actions, kemudian unduh dari tab **Actions → Artifacts**. Harus menempatkan file `app/`, `build.gradle.kts`, `settings.gradle.kts` dan `.github/` di root repository agar job berhasil. Status eksekusi workflow belum diverifikasi.

## Langkah menggunakan aplikasi (setelah berhasil terpasang)

1. `Asesmen`: isi sekolah, kelas, mapel, jumlah soal dan bobot, **kunci PG dan isian**, KKM, pilih `ScoreMode`.
2. `Simpan`: validasi kunci, total soal, rumus, dan versi penilaian.
3. `Cetak LJK`: aplikasi menulis PDF untuk soal PG dari asesmen; per lembar siswa tuliskan kode dan nama siswa sendiri.
4. `Koreksi`: isi kode dan nama, masukkan jawaban manual atau gunakan `Foto/Scan`. Setelah scan OMR, cocokkan hasil A-E dengan foto asli.
5. Masukkan hasil isian (satu per baris) dan nilai uraian dari guru (satu angka 0 sampai bobot per baris).
6. `Hitung & Simpan Draf` untuk penyimpanan sementara; `Verifikasi ... FINAL` hanya final bila semua butir sudah punya status sah.
7. `Rekap` menampilkan nilai final, status, dan link ke koreksi siswa.
8. `Ekspor` memilih lokasi penyimpanan melalui dialog Android dan menghasilkan Excel enam sheet.

### Format input jawaban

- PG: `A,B,C,D,?,A,-,B` harus **tepat sejumlah PG**. `?` ragu/tak terbaca (blokir FINAL), `-` **dikonfirmasi kosong** (poin 0). Jika jawaban ganda, `A+B` harus direview.
- Isian: satu jawaban per baris, `?` atau `-` untuk status khusus. Kunci bisa `4|empat`, tetapi semantik tetap diverifikasi guru.
- Uraian: setiap baris skor desimal `0..bobot`, termasuk `0` yang merupakan skor sah; `?` bila belum dinilai, `-` jika guru memastikan jawaban kosong.
- Soal yang **tidak memiliki input** (bukan `-`) tetap status `NOT_CAPTURED`, bukan salah otomatis.

### Mode dan formula

- `POIN`: `ROUND(100*(PG_POIN+IS_POIN+UR_POIN)/(PG_MAKS+IS_MAKS+UR_MAKS),2)`.
- `PROPORSI`: koefisien kategori PG/Isian/Uraian harus total **1,0**. Kategori tanpa soal aktif wajib memiliki bagian 0.
- `FORMULA_MANUAL`: koefisien kategori + bonus − pengurang, dibatasi 0..100.
- `CUSTOM_EXPR`: operasi `+ - * / ( )`, fungsi `MIN`, `MAX`, `ROUND`, `CLAMP`. Variabel `PG_POIN`, `IS_POIN`, `UR_POIN`, `PG_MAKS`, `IS_MAKS`, `UR_MAKS`, `PG_PERSEN`, `IS_PERSEN`, `UR_PERSEN`, `BENAR_PG`, `SALAH_PG`, `KOSONG_PG`, `BONUS`, `PENGURANG`, `KKM`.

Contoh: `CLAMP(0.5*PG_PERSEN+0.2*IS_PERSEN+0.3*UR_PERSEN+BONUS-PENGURANG,0,100)`.

Jika rumus tidak valid, aplikasi menolak menyimpan konfigurasi. Nilai final hanya muncul jika versi formula dan kunci sesuai dengan submission dan tidak ada jawaban belum diselesaikan.

## Cara menjalankan unit test mesin penilaian di komputer

Jika tersedia `kotlinc`:

```bash
kotlinc app/src/main/java/id/edukreativa/korektorku/domain/*.kt \
  app/src/main/java/id/edukreativa/korektorku/export/XlsxExporter.kt \
  tests/TestRunner.kt -include-runtime -d korektorku-tests.jar
java -jar korektorku-tests.jar /tmp/Export_Contoh.xlsx

kotlinc tests/AndroidGraphicsStubs.kt tests/MockSheetTemplate.kt \
  tests/OmrSyntheticTest.kt \
  app/src/main/java/id/edukreativa/korektorku/scan/OmrReader.kt \
  -include-runtime -d korektorku-omrtests.jar
java -jar korektorku-omrtests.jar
```

Mock grafis **hanya untuk pengujian JVM**, bukan kode yang dikompilasi ke APK.

## Risiko dan prasyarat rilis

- **Scanner eksperimental:** 40/40 pembacaan benar pada satu lembar sintetis ideal tanpa bayangan/perspektif, **bukan** bukti akurasi kamera di kelas. Perlu uji kertas cetak asli, variasi pencahayaan, perspektif, coretan, kamera, dan ukuran cetak; setelan tinta/kepercayaan harus dikalibrasi ulang.
- Penyimpanan JSON via `SharedPreferences` bukan basis data transaksi berskala; sebelum produksi ganti Room + audit trail, mekanisme migrasi, dan pemulihan dari data korup.
- Status `VERIFIED` berasal dari guru yang memilih tombol final; aplikasi belum mengautentikasi identitas guru.
- `ANALISIS_SOAL` menghitung persentase skor penuh dari lembar final. Untuk uraian bukan analisis miskonsepsi otomatis.
- Workbook Excel dan PDF bukan berkas terenkripsi; hanya ekspor siswa yang berwenang dan simpan sesuai kebijakan sekolah.
- Foto yang sudah disalin ke area privat tidak dihapus otomatis. Tambahkan kebijakan retensi/hapus sebelum penggunaan nyata.

Lihat `docs/PROGRESS_V3.md` untuk handoff rinci dan `docs/PEDOMAN_KOREKTORKU_TERINTEGRASI_V2.md` untuk spesifikasi lengkap versi sebelumnya.

## Perubahan v4

Lihat `docs/PROGRESS_V4.md`. Pemeriksaan marker OMR diperkuat, tes kasus negatif ditambahkan, dan setelah foto di-scan pengguna harus mengonfirmasi pemeriksaan visual sebelum menekan FINAL. Ini masih source yang belum dibangun menjadi APK.


## Perubahan v5 dan status penyelesaian

1. Konfirmasi guru atas foto dipersistenkan (`photoReviewed`), bukan sekadar status tombol sementara. Rekap nilai final ditahan jika foto belum ditinjau setelah aplikasi dibuka kembali.
2. Mesin nilai juga menahan PG `RECOGNIZED` yang belum `reviewed`, meskipun terdeteksi oleh sistem.
3. Tombol **Simpan FINAL** menolak transaksi saat ada butir/versi/foto yang belum sah; penyimpanan draf tetap tersedia.
4. Data disimpan dengan `SharedPreferences.commit()` sinkron dan satu snapshot cadangan. Jika snapshot utama rusak, aplikasi memulihkan cadangan dan mengingatkan bahwa data terbaru mungkin belum ikut. Ini **bukan** backup luar perangkat.
5. OMR memvalidasi jarak/geometri keempat penanda tambahan sebelum membaca gelembung.
6. Pengujian JVM regresi ditambah, dan workflow GitHub Actions menjalankan unit test Android dan build APK debug.
7. Impor OCR tulisan tangan dan skoring uraian otomatis **belum** diimplementasikan. Input isian/uraian guru tetap fungsional.

**Penting:** Android APK belum berhasil dikompilasi maupun diinstal pada perangkat pada sesi penyusunan source. Kode domain dapat diuji lewat `kotlinc`, namun Android Gradle build harus berhasil di GitHub Actions/Android Studio sebelum mengklaim APK siap digunakan. Aplikasi ini masih **MVP uji coba**, bukan rilis aman untuk produksi massal di sekolah.

### Tes regresi tambahan

```bash
P=app/src/main/java/id/edukreativa/korektorku
kotlinc "$P"/domain/*.kt tests/RegressionV5.kt -include-runtime -d /tmp/korektorku-v5-regression.jar
java -jar /tmp/korektorku-v5-regression.jar
```

Pada GitHub monorepo yang sudah berisi KOREKSIKU Web/PWA, simpan source Android dalam subfolder `android/` dan workflow pada `.github/workflows/android-debug.yml`. Workflow memilih path yang benar secara otomatis tanpa mengganggu tes PWA.
