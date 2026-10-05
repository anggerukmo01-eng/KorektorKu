# KOREKSIKU
Aplikasi Koreksi Jawaban & Nilai Siswa untuk SD NEGERI 1 GERDUREN.
Developer: **Edukreativa@Anggerukmo**.

## Menjalankan
Prasyarat: Node.js 20.19+ atau 22+, npm.

```bash
npm ci
npm run dev
```
Buka alamat localhost yang ditampilkan. Semua data siswa disimpan di IndexedDB pada browser dan alamat aplikasi tersebut. Tidak ada backend, login, analitik, atau pengiriman data siswa ke server.

## Build production dan PWA
```bash
npm test
npm run build
npm run preview
```
Folder `dist/` adalah aplikasi siap dihosting di server statis, termasuk di subfolder. Build production tersedia sebagai artefak `koreksiku-web` pada workflow GitHub Actions yang berhasil. Jangan membuka `index.html` langsung lewat `file://`; modul JavaScript dan service worker memerlukan server HTTP/HTTPS.

PWA dan offline aktif pada **build production**, bukan mode dev. Gunakan HTTPS pada hosting, atau localhost saat uji komputer. Buka aplikasi sekali ketika online sampai aset tersimpan. Di Chrome Android pilih menu **Instal aplikasi / Tambahkan ke layar utama**, atau tombol Instal aplikasi bila browser menyediakannya. Instalasi adalah PWA, bukan APK.

Data terpisah per perangkat/browser/alamat. Untuk memindahkan data dari dev ke production atau dari laptop ke tablet, gunakan Backup JSON dan Restore JSON. Jangan menghapus data browser tanpa backup. Pembaruan aplikasi di alamat sama mempertahankan IndexedDB; service worker baru aktif setelah tab versi lama ditutup.

## Alur guru
1. Periksa 20 siswa awal pada **Data Siswa**. Jenis kelamin sengaja kosong, tidak ditebak dari nama.
2. **Koreksi Data Siswa → Buat Tes Baru**. Isi identitas, jumlah PG/isian/uraian, opsi 2–5, dan bobot.
3. **Kunci Jawaban**: pilih jawaban PG, alternatif isian dipisahkan `;`, serta mode uraian.
4. **Koreksi**: pilih siswa. Isi PG dengan tombol/keyboard, atau masukkan `ABCAC...` / `A B C A C ...`. Gunakan `-` untuk jawaban kosong.
5. **Koreksi Sekarang** memfinalisasi hasil. Uraian manual harus diberi skor, termasuk 0 untuk kosong.
6. **Koreksi massal**: pilih siswa, gunakan tabel, Tab/Shift+Tab/Enter, lalu Koreksi siswa terpilih. Skor uraian manual dapat dimasukkan langsung tanpa menyalin teks jawaban dari kertas.
7. **Rekap Hasil**: pilih tes/mata pelajaran/kelas/siswa; export Excel/CSV, pratinjau, cetak atau simpan PDF melalui dialog cetak browser.
8. **Pengaturan → Backup / Export JSON** secara berkala. Restore mengganti seluruh database dan memerlukan konfirmasi.

## Perhitungan
Nilai = total poin × 100 / skor maksimal, dibulatkan dua desimal pada nilai akhir.
- PG: cocok dengan satu kunci, 0 atau poin penuh.
- Isian: cocok persis setelah normalisasi Unicode, kapital, dan spasi. Alternatif dipisahkan titik koma. Tidak ada fuzzy matching.
- Uraian manual: skor 0 sampai maksimum, termasuk pecahan. Belum diisi berbeda dari 0.
- Uraian otomatis: poin dibagi rata berdasarkan frasa kunci yang ditemukan sebagai kata/frasa utuh. Ini bantuan pemeriksaan, bukan penilaian semantik. Guru perlu memeriksa relevansinya.
- Poin sebagian dihitung dalam skor, tetapi hitungan soal dikelompokkan sebagai **salah / sebagian**. Benar berarti poin penuh. Kosong berarti jawaban kosong dan skor 0. Skor manual yang belum diisi menahan finalisasi.
- Contoh 10 PG × 1 + 5 isian × 2 + 5 uraian × 4 = 40. Perolehan 8 + 8 + 16 = 32 → nilai **80**.
- Perubahan jawaban membatalkan hasil final siswa terkait. Perubahan kunci/bobot membatalkan seluruh hasil final tes tersebut. Jawaban tetap tersimpan dan perlu dikoreksi ulang.
- Analisis butir memakai hasil yang sedang difilter. Ranking opsional memakai hasil yang ditampilkan; gunakan satu tes untuk perbandingan yang sejenis.

## Penyimpanan dan validasi
- Dexie/IndexedDB: students, subjects, tests, questions, answers, results, settings.
- Semua jawaban dan kunci disimpan setiap perubahan; indikator sukses hanya sesudah transaksi selesai. Tunggu indikator sebelum menutup aplikasi.
- Form tambah/edit siswa dan tes disimpan sebagai draft lokal sampai disimpan atau dibatalkan. Preferensi tampilan juga dicatat di LocalStorage.
- Input numerik tidak valid tidak disimpan; notifikasi meminta perbaikan.
- Backup memuat seluruh tabel. Restore memeriksa struktur, ID, relasi, rentang skor, opsi, dan menghitung ulang hasil sebelum transaksi atomik.
- Penghapusan siswa menghapus jawaban/nilai terkait setelah konfirmasi. Penghapusan tes menghapus soal, jawaban, hasilnya. Mata pelajaran yang dipakai tes tidak bisa dihapus.
- Duplikasi tes menyalin kunci dan konfigurasi, tanpa jawaban/nilai siswa.
- Pemulihan 20 nama awal hanya menambahkan nama yang belum ada; reset penuh ada di Pengaturan dengan dua tahap konfirmasi.

## Excel siswa
Kolom: `Nama`, `Absen`, `Kelas`, `Jenis Kelamin`, `Status`, `Catatan`.
Impor Excel menambah siswa; absen yang sama di kelas yang sama ditolak agar tidak menimpa siswa secara tidak sengaja. Gunakan Edit untuk memperbarui siswa lama. Nama dinormalisasi kapitalisasi dan titik yang tidak diperlukan dihapus. Export siswa bisa dipakai sebagai contoh format.

## Struktur
```
src/
  components/  komponen UI bersama
  pages/       Beranda, Siswa, Tes, Kunci, Koreksi, Rekap, Pengaturan
  db/          skema, seed, transaksi dan relasi
  data/        20 siswa, mata pelajaran, pengaturan awal
  services/    backup, restore, Excel, CSV
  utils/       scoring yang diuji dan draft form
scripts/       pembuat cache service worker production
tests/         pengujian Node untuk skor dan database
public/        ikon PWA dan manifest
```

## GitHub
Repositori: https://github.com/anggerukmo01-eng/KorektorKu

Workflow CI menjalankan `npm ci`, `npm test`, dan `npm run build` pada push/pull request, lalu menyimpan hasil build sebagai artefak `koreksiku-web`. Folder `node_modules/` dan `dist/` tidak dimasukkan ke Git. CI ini tidak memublikasikan website secara otomatis.

Untuk GitHub Pages: gunakan output `dist/` dari build sebagai artefak Pages. Aplikasi tidak membutuhkan secrets. Pastikan visibilitas repositori sesuai kebutuhan karena data awal nama siswa ada di `src/data/seed.js`.
