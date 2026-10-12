# PROGRESS PENGEMBANGAN — KOREKTORKU v3
Tanggal: 12 Oktober 2026  |  Milestone: source Android MVP offline, bukan APK final.

## Fitur yang dibuat
1. Proyek Android berbasis Kotlin tanpa perpustakaan eksternal saat runtime, screen Beranda/Asesmen/Koreksi/Rekap.
2. Form jumlah PG 0-50, isian 0-20, uraian 0-10; validasi kunci PG A-E dan alternatif isian, bobot per jenis/per nomor, KKM, proporsi, koefisien dan ekspresi khusus.
3. Mesin penilaian deterministik dan empat score mode; desimal `BigDecimal` dan rounding HALF_UP dua desimal; review status ganda/tak pasti/hilang dan versi key+formula.
4. Capture dengan camera intent dan penyimpanan privat melalui `ContentProvider`, impor gambar via file picker, preview foto, algoritma OMR eksperimental dengan empat fiducial pada LJK yang dihasilkan sendiri.
5. Generate PDF LJK dalam app (50 PG maksimal), ekspor XLSX enam sheet menggunakan `ZipOutputStream` dan OpenXML, rekapan hanya final. Pencegahan injection sel karena seluruh teks sumber diekspor sebagai `inlineStr`.
6. Konfigurasi dan hasil disimpan lokal sebagai JSON di SharedPreferences; versi lama submission tidak auto final jika konfigurasi berubah.

## Jejak uji aktual (bukan klaim build Android)
- Domain + eksportir dapat dikompilasi `kotlinc` sebagai JVM normal.
- 14 assertion: POIN 83.08; PROPORSI 82.40; FORMULA_MANUAL 82.40; CUSTOM_EXPR 82.40; unresolved flag; nilai nol; versi kunci; penolakan formula salah / pembagian nol / fungsi asing; penolakan bobot dan skor tak sah.
- Export contoh `.xlsx` diuji sebagai ZIP valid; semua XML valid; enam worksheet; cell siswa yang diawali `=HYPERLINK` tetap teks.
- Import `.xlsx` berhasil diuji memakai artefak parser Excel: 6 sheet, rekap dua siswa, nilai 83.08 pada FINAL dan sel kosong pada PERLU REVIEW.
- OMR 4 marker dari satu raster sintetis; 40/40 pilihan cocok dalam kondisi ideal.
- **Belum diuji:** Gradle/Android SDK (tidak tersedia), instalasi APK, UI pada perangkat, PDF app pada HP, foto LJK nyata, backup dan keamanan produksi.

## Keterbatasan/pekerjaan berikutnya
- Build Android dengan JDK 17 + AGP 8.7.3 + Gradle 8.9 + SDK35, jalankan lint/assembleDebug; perbaiki jika ada kegagalan sinkronisasi atau compile.
- Tes foto LJK asli pada berbagai perangkat: kertas miring, bayangan, underexposed, gelembung separuh, tanda X, jawaban ganda; tingkatkan algoritma perspektif dengan homography dan validasi geometri.
- OCR untuk isian harus diimplementasikan dan diverifikasi oleh guru; uraian diberi antarmuka rubrik multi-kriteria.
- Pindahkan JSON SharedPreferences ke Room agar data siswa, kunci, versi, audit, relasi dan transaksi lebih stabil; tambahkan PIN/biometrik dan retensi file.
- Tambah identitas LJK berbasis QR ID acak, template versi, scan hash anti-duplikasi, kelas dan paket A/B, ekspor workbook formula spreadsheet yang dapat diubah dan disinkronkan secara resmi.
- Perbanyak uji UI/instrumented dan kompatibilitas Android; **jangan distribusikan sebagai produk sekolah sebelum lulus tes lapangan/privasi**.

## Keputusan desain
MVP mengutamakan mesin nilai teruji + input guru + ekspor, sedangkan OMR ditandai eksperimen dan OCR belum ada. Tidak mengklaim pengenalan tulisan tangan/esai otomatis pada aplikasi saat ini.

## Pengujian APK
Tidak ada APK pada folder ini: belum ada Android SDK atau Gradle di lingkungan pembuatan dokumen. Workflow GitHub Actions disertakan untuk menghasilkan debug APK setelah source diunggah dan job sukses.
