package com.harshita.iykykcollage.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceEmbedder(context: Context) : AutoCloseable {
    private val interpreter: Interpreter
    private val inputSize: Int
    private val outputSize: Int

    init {
        val descriptor = context.assets.openFd(MODEL_NAME)
        val model = FileInputStream(descriptor.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength
        )
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
        inputSize = interpreter.getInputTensor(0).shape()[1]
        outputSize = interpreter.getOutputTensor(0).shape().last()
    }

    fun embed(face: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(face, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        pixels.forEach { pixel ->
            input.putFloat((((pixel shr 16) and 0xFF) - 127.5f) / 128f)
            input.putFloat((((pixel shr 8) and 0xFF) - 127.5f) / 128f)
            input.putFloat(((pixel and 0xFF) - 127.5f) / 128f)
        }
        val output = Array(1) { FloatArray(outputSize) }
        interpreter.run(input.rewind(), output)
        return l2Normalize(output[0])
    }

    override fun close() = interpreter.close()

    private fun l2Normalize(values: FloatArray): FloatArray {
        val norm = sqrt(values.sumOf { (it * it).toDouble() }).toFloat().coerceAtLeast(1e-12f)
        return FloatArray(values.size) { values[it] / norm }
    }

    companion object { const val MODEL_NAME = "mobile_face_net.tflite" }
}
