package com.harshita.iykykcollage.processing

import android.graphics.*
import com.harshita.iykykcollage.model.PersonResult
import kotlin.math.ceil

object CollageGenerator {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val PAD = 48f

    fun create(people: List<PersonResult>): Bitmap {
        val output = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bg = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), Color.rgb(255,247,238), Color.rgb(235,229,255), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), Paint().apply { shader = bg })
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35,28,52); textSize = 76f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("people in this moment", PAD, 125f, title)
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(92,82,112); textSize = 34f }
        canvas.drawText("${people.size} unique ${if (people.size == 1) "person" else "people"} • ${people.sumOf { it.appearanceCount }} appearances", PAD, 180f, subtitle)

        val columns = if (people.size <= 2) 1 else 2
        val rows = ceil(people.size / columns.toDouble()).toInt().coerceAtLeast(1)
        val gap = 24f
        val tileWidth = (WIDTH - PAD * 2 - gap * (columns - 1)) / columns
        val availableHeight = HEIGHT - 270f - PAD
        val tileHeight = minOf(540f, (availableHeight - gap * (rows - 1)) / rows)
        people.forEachIndexed { index, person ->
            val col = index % columns; val row = index / columns
            val left = PAD + col * (tileWidth + gap); val top = 245f + row * (tileHeight + gap)
            drawTile(canvas, person, RectF(left, top, left + tileWidth, top + tileHeight))
        }
        canvas.drawText("FRAMEFRIENDS", PAD, HEIGHT - 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 60, 50, 80); textSize = 24f; letterSpacing = 0.2f })
        return output
    }

    private fun drawTile(canvas: Canvas, person: PersonResult, rect: RectF) {
        val radius = 34f
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; setShadowLayer(18f, 0f, 8f, Color.argb(45,0,0,0)) })
        val imageRect = RectF(rect.left + 10, rect.top + 10, rect.right - 10, rect.bottom - 78)
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(imageRect, radius - 8f, radius - 8f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        val src = centerCropSource(person.representative, imageRect.width() / imageRect.height())
        canvas.drawBitmap(person.representative, src, imageRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)); canvas.restore()
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42,34,58); textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("Person ${person.id + 1}", rect.left + 24, rect.bottom - 30, label)
        val count = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(108,92,231); textSize = 27f; textAlign = Paint.Align.RIGHT }
        canvas.drawText("${person.appearanceCount}×", rect.right - 24, rect.bottom - 30, count)
    }

    private fun centerCropSource(bitmap: Bitmap, targetRatio: Float): Rect {
        val ratio = bitmap.width.toFloat() / bitmap.height
        return if (ratio > targetRatio) {
            val w = (bitmap.height * targetRatio).toInt(); val l = (bitmap.width - w) / 2
            Rect(l, 0, l + w, bitmap.height)
        } else {
            val h = (bitmap.width / targetRatio).toInt(); val t = (bitmap.height - h) / 2
            Rect(0, t, bitmap.width, t + h)
        }
    }
}
