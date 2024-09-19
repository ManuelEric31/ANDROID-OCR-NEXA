package com.internship.ocrnexa.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val boxRect = RectF()

    val boxRatio = 0.7f
    private val verticalOffset = 0.3f
    private val ktpAspectRatio = 85.6f / 53.98f


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val boxWidth = width * boxRatio
        val boxHeight = boxWidth / ktpAspectRatio

        val left = (width - boxWidth) / 2
        val top = height * verticalOffset
        val right = left + boxWidth
        val bottom = top + boxHeight

        boxRect.set(left, top, right, bottom)
        canvas.drawRect(boxRect, paint)
    }

    fun getBoxRect(): RectF = boxRect
}