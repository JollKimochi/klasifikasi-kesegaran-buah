package com.kelompok1.kesegaranbuah

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.kelompok1.kesegaranbuah.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var klasifikasi: KlasifikasiBuah? = null
    private var uriFoto: Uri? = null

    private val ambilFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { sukses ->
        val uri = uriFoto
        if (sukses && uri != null) proses(uri)
    }

    private val pilihGaleri = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) proses(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progres.visibility = View.GONE
        binding.kartuHasil.visibility = View.GONE
        binding.tombolKamera.isEnabled = false
        binding.tombolGaleri.isEnabled = false

        binding.tombolKamera.setOnClickListener { bukaKamera() }
        binding.tombolGaleri.setOnClickListener { pilihGaleri.launch("image/*") }

        muatModel()
    }

    private fun muatModel() {
        lifecycleScope.launch {
            val hasil = withContext(Dispatchers.IO) {
                runCatching { KlasifikasiBuah(applicationContext) }
            }
            hasil.onSuccess { model ->
                klasifikasi = model
                binding.tombolKamera.isEnabled = true
                binding.tombolGaleri.isEnabled = true
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    "Model gagal dimuat. Pasang ulang aplikasi.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun bukaKamera() {
        val folder = File(cacheDir, "foto").apply { mkdirs() }
        val berkas = File.createTempFile("buah_", ".jpg", folder)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", berkas)
        uriFoto = uri
        ambilFoto.launch(uri)
    }

    private fun proses(uri: Uri) {
        val model = klasifikasi ?: return
        binding.progres.visibility = View.VISIBLE
        binding.kartuHasil.visibility = View.GONE

        lifecycleScope.launch {
            val hasil = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = muatBitmap(uri)
                    val mulai = System.currentTimeMillis()
                    val prediksi = model.klasifikasi(bitmap)
                    val durasi = System.currentTimeMillis() - mulai
                    Triple(bitmap, prediksi, durasi)
                }
            }
            binding.progres.visibility = View.GONE

            hasil.onSuccess { (bitmap, prediksi, durasi) ->
                binding.gambarPratinjau.setImageBitmap(bitmap)
                findViewById<View>(R.id.ikonPlaceholder)?.visibility = View.GONE
                tampilkanHasil(prediksi, durasi)
            }.onFailure { galat ->
                Toast.makeText(
                    this@MainActivity,
                    "Gagal memproses gambar: ${galat.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun tampilkanHasil(prediksi: KlasifikasiBuah.Hasil, durasiMs: Long) {
        val (kondisi, buah) = KlasifikasiBuah.labelTampilan(prediksi.label)
        val persen = String.format(Locale.getDefault(), "%.1f%%", prediksi.keyakinan * 100f)

        binding.teksHasil.text = if (buah.isEmpty()) kondisi else "$buah, $kondisi"
        binding.teksKeyakinan.text = "Keyakinan $persen, diproses $durasiMs ms"

        // Hasil diberi warna sesuai kondisi: hijau muda untuk segar, merah muda
        // untuk tidak segar. Keduanya terang supaya terbaca di kartu gelap.
        val warnaHasil = if (kondisi.contains("Tidak", ignoreCase = true)) {
            R.color.hasil_tidak_segar
        } else {
            R.color.hasil_segar
        }
        binding.teksHasil.setTextColor(androidx.core.content.ContextCompat.getColor(this, warnaHasil))

        binding.kartuHasil.visibility = View.VISIBLE
    }

    private fun muatBitmap(uri: Uri): Bitmap {
        val resolver = contentResolver

        val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }

        val sisiMaks = 1024
        var skala = 1
        while (batas.outWidth / (skala * 2) >= sisiMaks || batas.outHeight / (skala * 2) >= sisiMaks) {
            skala *= 2
        }

        val opsi = BitmapFactory.Options().apply { inSampleSize = skala }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
            ?: throw IllegalArgumentException("Gambar tidak bisa dibaca")

        val derajat = resolver.openInputStream(uri)?.use { aliran ->
            val orientasi = ExifInterface(aliran)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientasi) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (derajat == 0f) return bitmap

        val matriks = Matrix().apply { postRotate(derajat) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriks, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        klasifikasi?.tutup()
    }
}
