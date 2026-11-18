# Klasifikasi Kesegaran Buah

Aplikasi Android native untuk menilai apakah sebuah buah masih **segar** atau sudah **tidak segar** dari satu foto. Dibuat untuk tugas mata kuliah Kecerdasan Buatan dengan pendekatan CNN.

## Fitur

- Foto langsung dari kamera atau pilih dari galeri.
- Klasifikasi berjalan sepenuhnya di perangkat memakai TensorFlow Lite, foto tidak diunggah.
- Hasil ditampilkan sebagai **Segar** atau **Tidak Segar** beserta tingkat keyakinannya.
- Ukuran masukan model dibaca otomatis dari berkas `.tflite`, tidak ditulis mati di kode.

## Cara kerja model

- CNN dilatih dari nol di Google Colab dengan citra 150 x 150 piksel.
- Normalisasi `Rescaling(1./255)` berada di dalam model, sehingga aplikasi mengirim piksel mentah 0 sampai 255.
- Keluaran satu nilai sigmoid: probabilitas kelas **tidak segar**. Nilai di bawah 0,5 berarti segar.
- Model diekspor ke TFLite dengan kuantisasi float16 sehingga berkasnya sekitar 9,6 MB.

## Struktur

```
klasifikasi_buah/
└── app-android/
    ├── app/src/main/
    │   ├── java/com/kelompok1/kesegaranbuah/
    │   │   ├── MainActivity.kt      # kamera, galeri, tampilan hasil
    │   │   └── KlasifikasiBuah.kt   # pembungkus TensorFlow Lite
    │   ├── res/                     # layout, tema, ikon
    │   └── assets/
    │       ├── model.tflite
    │       └── labels.txt
    └── build.gradle.kts
```

## Build

Persyaratan: JDK 17, Android SDK 35, Gradle wrapper sudah disertakan.

```bash
cd app-android
./gradlew assembleDebug
```

APK dihasilkan di `app-android/app/build/outputs/apk/debug/app-debug.apk`. Versi siap pasang juga dibagikan lewat halaman Releases.

## Spesifikasi

| Bagian | Nilai |
| --- | --- |
| Bahasa | Kotlin |
| Minimum Android | 7.0 (SDK 24) |
| Target/compile SDK | 35 |
| Runtime model | TensorFlow Lite |
| Masukan model | float32 `[1, 150, 150, 3]`, piksel 0–255 |
| Keluaran model | float32 `[1, 1]` (sigmoid) |
