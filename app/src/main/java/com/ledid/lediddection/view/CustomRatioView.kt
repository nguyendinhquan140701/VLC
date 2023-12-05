package com.ledid.lediddection.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ledid.lediddection.R

class CustomRatioView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var ratio: Float = 1f

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomRatioView)
        val ratioValue = typedArray.getString(R.styleable.CustomRatioView_whRatio)
        typedArray.recycle()

        if (!ratioValue.isNullOrBlank()) {
            val parts = ratioValue.split(":")
            if (parts.size == 2) {
                val numerator = parts[0].toFloatOrNull()
                val denominator = parts[1].toFloatOrNull()

                if (numerator != null && denominator != null && denominator != 0f) {
                    ratio = numerator / denominator
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = (widthSize / ratio).toInt()

        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        )
    }
}