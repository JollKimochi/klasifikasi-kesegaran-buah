package com.kelompok1.kesegaranbuah

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * Pembungkus model TensorFlow Lite.
 *
 * Kontrak model:
 * - Masukan  : float32, bentuk [1, tinggi, lebar, 3], nilai piksel 0..255
 * - Keluaran : float32. Kalau 1 nilai, itu probabilitas kelas kedua (sigmoid).
 *              Kalau lebih dari 1 nilai, dianggap skor per kelas (softmax).
 */
class KlasifikasiBuah(context: Context, namaModel: String = "model.tflite") {

    data class Hasil(
        val label: String,
        val keyakinan: Float,
        val skor: Map<String, Float>
    )

    private val interpreter: Interpreter
    private val label: List<String>
    private val ukuranKeluaran: Int

    val lebar: Int
    val tinggi: Int
    val jumlahKelas: Int

    init {
        val byteModel = context.assets.open(namaModel).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(byteModel.size).order(ByteOrder.nativeOrder())
        buffer.put(byteModel)
        buffer.rewind()
        interpreter = Interpreter(buffer)

        val tensorMasukan = interpreter.getInputTensor(0)
        if (tensorMasukan.dataType() != DataType.FLOAT32) {
            throw IllegalStateException(
                "Model harus memakai masukan float32, bukan ${tensorMasukan.dataType()}. " +
                    "Ekspor ulang tanpa kuantisasi int8."
            )
        }
        val bentukMasukan = tensorMasukan.shape()
        if (bentukMasukan.size != 4 || bentukMasukan[3] != 3) {
            throw IllegalStateException("Bentuk masukan model tidak dikenali: ${bentukMasukan.joinToString()}")
        }
        tinggi = bentukMasukan[1]
        lebar = bentukMasukan[2]

        ukuranKeluaran = interpreter.getOutputTensor(0).shape().last()
        jumlahKelas = if (ukuranKeluaran == 1) 2 else ukuranKeluaran

        label = muatLabel(context, jumlahKelas)
    }

    private fun muatLabel(context: Context, jumlah: Int): List<String> {
        val dariBerkas = try {
            context.assets.open("labels.txt").bufferedReader().use { pembaca ->
                pembaca.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { if (it.contains('\t')) it.substringAfter('\t').trim() else it }
            }
        } catch (e: Exception) {
            emptyList()
        }

        if (dariBerkas.size == jumlah) return dariBerkas
        if (jumlah == 2) return listOf("Segar", "Tidak Segar")
        return (0 until jumlah).map { "Kelas $it" }
    }

    fun klasifikasi(bitmap: Bitmap): Hasil {
        val diskala = Bitmap.createScaledBitmap(bitmap, lebar, tinggi, true)
        val piksel = IntArray(lebar * tinggi)
        diskala.getPixels(piksel, 0, lebar, 0, 0, lebar, tinggi)

        val masukan = ByteBuffer
            .allocateDirect(lebar * tinggi * 3 * 4)
            .order(ByteOrder.nativeOrder())

        for (warna in piksel) {
            masukan.putFloat(((warna shr 16) and 0xFF).toFloat())
            masukan.putFloat(((warna shr 8) and 0xFF).toFloat())
            masukan.putFloat((warna and 0xFF).toFloat())
        }
        masukan.rewind()

        val keluaran = Array(1) { FloatArray(ukuranKeluaran) }
        interpreter.run(masukan, keluaran)
        val mentah = keluaran[0]

        val skor = if (ukuranKeluaran == 1) {
            val p = mentah[0].coerceIn(0f, 1f)
            floatArrayOf(1f - p, p)
        } else {
            softmax(mentah)
        }

        var indeksTerbaik = 0
        for (i in skor.indices) {
            if (skor[i] > skor[indeksTerbaik]) indeksTerbaik = i
        }

        val peta = label.indices.associate { label[it] to skor[it] }
        return Hasil(
            label = label[indeksTerbaik],
            keyakinan = skor[indeksTerbaik],
            skor = peta
        )
    }

    private fun softmax(nilai: FloatArray): FloatArray {
        val total = nilai.sum()
        if (kotlin.math.abs(total - 1f) <= 0.01f) return nilai

        val maks = nilai.max()
        val eks = FloatArray(nilai.size) { exp((nilai[it] - maks).toDouble()).toFloat() }
        val jumlah = eks.sum()
        return FloatArray(nilai.size) { eks[it] / jumlah }
    }

    fun tutup() {
        interpreter.close()
    }

    companion object {
        /** Ubah nama folder dataset menjadi teks yang enak dibaca. */
        fun labelTampilan(nama: String): Pair<String, String> {
            val teks = nama.lowercase()
            val kondisi = when {
                teks.contains("fresh") -> "Segar"
                teks.contains("rotten") -> "Busuk"
                else -> nama.replaceFirstChar { it.uppercase() }
            }
            val buah = when {
                teks.contains("apple") -> "Apel"
                teks.contains("banana") -> "Pisang"
                teks.contains("orange") -> "Jeruk"
                teks.contains("mango") -> "Mangga"
                teks.contains("grape") -> "Anggur"
                teks.contains("strawberry") -> "Stroberi"
                else -> ""
            }
            return kondisi to buah
        }
    }
}
