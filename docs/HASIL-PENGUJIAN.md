# Hasil pengujian KOREKSIKU
Tanggal: 5 Oktober 2026.

## Pengujian otomatis: 12 lulus, 0 gagal
Perintah yang dapat diulang: `npm test`.

1. 10 PG dengan tiga opsi A/B/C → nilai 100 untuk semua benar.
2. 10 PG + 5 isian → skor maksimal 20.
3. 10 PG + 5 isian + 5 uraian → 8 + 8 + 16 = 32/40 → nilai 80.
4. Bobot berbeda tiap nomor → perhitungan sesuai bobot, bukan jumlah benar saja.
5. Benar, salah, dan kosong dihitung terpisah.
6. Isian kapitalisasi, spasi berlebih, dan alternatif angka.
7. Uraian manual membedakan belum diisi dan skor 0; skor di luar batas ditolak.
8. Uraian otomatis frasa utuh dan poin proporsional.
9. Input cepat berantai/berjarak; jumlah dan opsi tidak sesuai ditolak.
10. Kunci hilang, opsi tidak valid, dan skor melebihi maksimum ditolak.
11. Koreksi 20 siswa dan analisis butir (18/20 benar = 90%).
12. Seed tidak berulang, buka ulang database, backup/restore atomik, penolakan relasi rusak tanpa mengubah database, duplikasi tes tanpa jawaban, invalidasi hasil, dan penghapusan relasi.

Pengujian database Node menggunakan fake-indexeddb. Persistensi browser juga diuji terpisah dengan Chromium nyata.

## Pengujian browser Chromium
- Membuat tes 10 PG + 5 isian + 5 uraian melalui antarmuka.
- Mengisi kunci PG dan alternatif isian.
- Memasukkan jawaban berantai dan nilai uraian.
- Hasil aktual: skor 32/40, nilai 80.
- Memuat ulang browser: data dan hasil tetap ada.
- Mengunduh Excel; file dibaca ulang: satu baris, skor 32, nilai 80.
- Koreksi massal 20 siswa lewat tabel: 20 hasil final tersimpan; 100 skor uraian manual tersimpan.
- Backup JSON memuat 20 siswa dan 20 hasil.
- Restore file backup melalui antarmuka di konteks browser baru: 20 hasil dipulihkan.
- Service worker production meng-cache 7 aset.
- Browser diubah ke mode offline lalu dimuat ulang: halaman terbuka, 20 hasil tetap tersedia.
- Tema terang/gelap dapat dipilih.
- Tampilan desktop 1440 px, tablet landscape 1365 px, dan ponsel 393 px diperiksa. Beranda ponsel tidak menyebabkan overflow horizontal halaman; tabel lebar memiliki scroll sendiri.
- Tidak ada page error pada alur browser yang berhasil.
- Masalah draft tes yang muncul kembali setelah disimpan ditemukan dan diperbaiki.

## Build
`npm run build` berhasil. Build Vite dan cache service worker ada dalam `dist/`.
Ada peringatan ukuran satu bundle JavaScript sekitar 819 kB sebelum gzip (sekitar 266 kB setelah gzip); bukan kegagalan build. Bundle memuat pembaca/penulis Excel dan seluruh aplikasi agar tersedia offline.

## Batas verifikasi
- Belum diuji langsung pada unit fisik Xiaomi Pad 7 Pro atau dialog instalasi PWA Android.
- Cetak/PDF menggunakan dialog cetak browser; hasil pada printer fisik belum diverifikasi.
- Uraian otomatis menggunakan pencocokan frasa, tidak memahami makna jawaban.
- Laporan ini mencatat pengujian lokal pada 5 Oktober 2026. Source code diintegrasikan ke anggerukmo01-eng/KorektorKu pada 12 Oktober 2026; hasil CI terbaru tersedia di tab Actions. Website belum dipublikasikan.
- Data pengujian tidak disertakan sebagai data awal aplikasi. Aplikasi baru hanya memuat 20 siswa, lima mata pelajaran, dan identitas awal yang diminta.
