# KorektorKu v4 — Handoff dan Verifikasi

## Perubahan aktual
- OMR: menghapus batas antrean komponen 6000 piksel yang sebelumnya bisa memotong deteksi pada bercak besar; buffer dibuat sesuai ukuran bitmap.
- OMR: menolak rasio foto ekstrem dan pemeriksaan penanda yang tidak lengkap.
- UI: setelah scanner menimpa input PG, tombol FINAL dikunci sampai guru menekan konfirmasi foto/jawaban.
- Tes: mencakup OMR sintetis 40 soal, foto tanpa marker, rasio foto ekstrem, formula dan penghitungan.

## Keterbatasan saat ini
- Pengujian di JVM tidak sama dengan pengujian Android device. Belum tersedia Android SDK/Gradle sehingga APK belum diproduksi di sesi ini.
- Konfirmasi foto manual tidak menggantikan validasi karakter dari algoritma vision.
- Foto nyata dan OCR isian/uraian masih perlu pengembangan.
- Source ini perlu diuji Gradle sync/build, crash log, dan keamanan data sekolah sebelum dipakai secara produksi.

## Build
Di Android Studio dengan SDK 35 dan JDK 17, buka root project lalu jalankan `gradle :app:assembleDebug`, atau unggah source ke GitHub agar workflow Actions berjalan. APK hanya boleh dianggap valid setelah workflow berhasil dan instalasi perangkat sukses.
