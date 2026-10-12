# HANDOFF KOREKTORKU ANDROID v5 — 12 Oktober 2026

## Status produk

Proyek Android native Kotlin ini **source MVP untuk pengujian**, bukan APK yang terkonfirmasi siap instal. Android SDK dan Gradle tidak tersedia pada lingkungan pembuatan. Percobaan mengakses distribusi Gradle/Android command-line tools dari container gagal pada DNS. Build dan tes di Android asli **belum** dijalankan. Jangan mengklaim telah membangun APK hanya dari kesuksesan tes domain Kotlin.

## Yang benar-benar diimplementasikan

- Asesmen kombinasi PG (0–50), isian (0–20), uraian (0–10), kunci PG A–E, alternatif isian, bobot per jenis dan per butir.
- Skor poin, proporsi, koefisien manual dan parser formula khusus dengan whitelist.
- Review manual dan status draf/FINAL, KKM, versi kunci/formula, skor uraian 0–maksimum.
- Template PDF LJK PG 4 penanda, kamera aplikasi eksternal / galeri, pembaca gelembung berbasis piksel **eksperimental**.
- Konfirmasi foto guru persisten, penolakan finalisasi yang belum lengkap atau kunci kedaluwarsa.
- Simpan data secara offline di SharedPreferences internal, pencadangan satu versi terakhir jika snapshot rusak.
- Ekspor rekap nilai `.xlsx` enam sheet menggunakan ZIP/OpenXML, sebagai snapshot tanpa rumus Excel yang bisa diedit.

## Bukti pengujian

| Pengujian | Hasil yang diuji | Batasan |
|---|---|---|
| Kotlin domain TestRunner | Lulus; sampel 83,08 dan 82,40 | Tanpa Android UI |
| Regresi v5 | Lulus; hasil scan belum dikonfirmasi menahan final, skor nol sah, versi berubah ditahan | Tanpa Android UI |
| OMR sintetis | 40/40 cocok | Satu gambar sintetis ideal |
| Kasus negatif OMR | Penanda hilang, rasio ekstrem, geometri palsu ditolak | Belum menguji kamera nyata |
| Ekspor OpenXML | Berkas ZIP `.xlsx` dihasilkan | Harus dibuka juga di Excel perangkat pengguna |
| JUnit Android & APK | Belum dapat dijalankan lokal | Workflow GitHub telah disiapkan |

## Pekerjaan tersisa menuju penggunaan sekolah

1. Commit/push branch Android ke GitHub; jalankan workflow GitHub Actions sampai `:app:testDebugUnitTest` dan `:app:assembleDebug` berhasil. Unduh APK debug hanya dari artifact build sukses.
2. Tes instalasi dan peluncuran pada beberapa perangkat Android; cek kamera, galeri, dokumen SAF, izin URI, ruang penyimpanan, rotasi, OCR di masa depan.
3. Uji kalibrasi OMR pada sedikitnya puluhan lembar cetak dari beberapa kamera dan kondisi pencahayaan berbeda; kumpulkan false positive dan false negative sebelum bergantung pada hasil scan.
4. Tambahkan audit trail, autentikasi/PIN dan manajemen penghapusan data siswa, kebijakan foto, ekspor backup aman sebelum rilis sekolah.
5. OCR isian foto yang terbaca, review hasil OCR wajib; penilaian uraian sebaiknya tetap memerlukan guru/rubrik untuk menghindari salah skor otomatis.
6. Build signed release APK/AAB baru setelah verifikasi perangkat, keamanan dan privasi; debug APK hanya untuk pengujian.

## Cara build

Di Android Studio, pilih proyek yang mengandung `settings.gradle.kts`, JDK 17, Gradle 8.9, SDK platform 35, jalankan `gradle :app:testDebugUnitTest :app:assembleDebug`.

Di repositori GitHub `anggerukmo01-eng/KorektorKu`, proyek saat ini adalah web/PWA. Agar aman, letakkan source ini dalam `android/` dan tambahkan workflow Android di `.github/workflows/android-debug.yml`. Jangan menimpa file web/PWA. Buka Pull Request dari branch fitur; artifact APK akan ada di workflow GitHub Actions yang sukses.
