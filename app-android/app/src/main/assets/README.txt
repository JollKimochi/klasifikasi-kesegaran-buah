Letakkan dua berkas ini di folder assets sebelum menjalankan aplikasi:

1. model.tflite  - model hasil ekspor dari notebook Colab
2. labels.txt    - daftar label, satu per baris, contoh:
   0	Segar
   1	Busuk

Kontrak model yang dipakai aplikasi:
- Masukan : float32, bentuk [1, tinggi, lebar, 3], nilai piksel 0 sampai 255
- Keluaran: float32, satu nilai (sigmoid) atau beberapa nilai (softmax)

Kalau labels.txt tidak ada, aplikasi memakai "Segar" dan "Busuk" untuk model dua kelas.
