# Proyek Android: Klasifikasi Kesegaran Buah

Modul Android dari tugas klasifikasi kesegaran buah. Aplikasi memakai model CNN yang sudah dilatih di Colab dan diekspor ke TensorFlow Lite.

## Cara kerja

1. Pengguna memotret buah atau memilih foto dari galeri.
2. Gambar diperkecil ke ukuran yang diminta model, dibaca otomatis dari berkas `.tflite`.
3. Model memberi satu angka: probabilitas **tidak segar**.
4. Hasil ditampilkan sebagai "Segar" atau "Tidak Segar" beserta persentase keyakinan.

## Berkas model

Dua berkas di `app/src/main/assets/`:

- `model.tflite`: hasil ekspor dari Colab (float16).
- `labels.txt`: daftar label, satu per baris:

```
0	Segar
1	Tidak Segar
```

Kontrak model: masukan `float32` bentuk `[1, tinggi, lebar, 3]` dengan nilai piksel 0 sampai 255, keluaran `float32` satu nilai sigmoid. Aplikasi juga menerima keluaran softmax beberapa nilai.

## Build

Dari folder `app-android`:

```bash
./gradlew assembleDebug
```

APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

Pasang ke HP yang terhubung lewat USB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Atau buka folder `app-android` di Android Studio, lalu `Run`.

## Struktur

```
app/src/main/
├── java/com/kelompok1/kesegaranbuah/
│   ├── MainActivity.kt        # kamera, galeri, tampilan hasil
│   └── KlasifikasiBuah.kt     # pembungkus TensorFlow Lite
├── res/                       # layout dan tema
└── assets/                    # model.tflite dan labels.txt
```
