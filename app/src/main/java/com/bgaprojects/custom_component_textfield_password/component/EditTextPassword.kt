package com.bgaprojects.custom_component_textfield_password.component

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputFilter
import android.text.TextPaint
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.bgaprojects.custom_component_textfield_password.R
import com.bgaprojects.custom_component_textfield_password.component.utils.CoroutinesUtil
import com.bgaprojects.custom_component_textfield_password.component.utils.setTextAppearance
import kotlinx.coroutines.MainScope
import java.lang.Float.max
import kotlin.math.min

class EditTextPassword @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var cxOffset: Int = 0
    private val passwordTextPaint = TextPaint()
    private val filledCirclePaint = Paint()
    private val outlinedCirclePaint = Paint().apply {
        style = Paint.Style.STROKE
    }
    private var circleRadius = 0f
    private val circleDiameter by lazy {
        circleRadius * 2
    }
    private var charMargin = 0
    var passwordLength = 6
        set(value) {
            field = value
            setupMaxDigits()
            requestLayout()
        }
    var showPassword = false
        set(value) {
            field = value
            hideLasChar = true
            setupTransformationMethod()
            invalidate()
        }
    private val scope = MainScope()
    private var hideLasChar = true
    private val passwordVisibilityHandler = CoroutinesUtil.debounce(
        waitMs = CHAR_VISIBILITY_TIME, coroutineScope = scope
    ) {
        hideLasChar = true
        invalidate()
    }

    init {
        setBackgroundResource(android.R.color.transparent)
        setup(
            context.obtainStyledAttributes(attrs, R.styleable.PasswordEditText, defStyleAttr, 0)
        )
    }

    private fun setup(typedArray: TypedArray) {

        charMargin = typedArray.getDimensionPixelSize(
            R.styleable.PasswordEditText_password_charMargin, 0
        )

        circleRadius =
            typedArray.getDimension(R.styleable.PasswordEditText_password_circleRadius, 0f)

        passwordTextPaint.setTextAppearance(
            context, typedArray, R.styleable.PasswordEditText_password_charStyle
        )

        outlinedCirclePaint.strokeWidth = typedArray.getDimension(
            R.styleable.PasswordEditText_password_circleBorderWidth, 0f
        )

        outlinedCirclePaint.color = typedArray.getColor(
            R.styleable.PasswordEditText_password_circleColor, Color.WHITE
        )

        filledCirclePaint.color = outlinedCirclePaint.color

        typedArray.recycle()
    }

    private fun setupMaxDigits() {
        filters = arrayOf(InputFilter.LengthFilter(passwordLength))
    }

    private fun getTextLength() = text?.length ?: 0

    private fun setupTransformationMethod() {
        transformationMethod = if (!showPassword) {
            PasswordTransformationMethod()
        } else {
            null
        }

        // change transformation must move the selection cursor
        onSelectionChanged(0, getTextLength())
    }

    override fun onSelectionChanged(start: Int, end: Int) {
        val currentText = text

        if (currentText != null && (start != currentText.length || end != currentText.length)) {
            setSelection(currentText.length, currentText.length)
            return
        }

        super.onSelectionChanged(start, end)
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)

        if (isAttachedToWindow) {
            hideLasChar = lengthAfter <= lengthBefore
            ::passwordVisibilityHandler
        }
    }

    private fun getDesiredHeight(): Int {
        val circleHeight = circleDiameter + (2 * outlinedCirclePaint.strokeWidth)
        val fontHeight = passwordTextPaint.fontMetrics.run {
            bottom - top + leading
        }

        return max(circleHeight, fontHeight).toInt()
    }

    private fun getDesiredWidth() =
        ((circleDiameter * passwordLength) + ((passwordLength - 1) * charMargin) + (2 * outlinedCirclePaint.strokeWidth)).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = getDesiredWidth()
        val desiredHeight = getDesiredHeight()
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        calculateCxOffset(desiredWidth, width)

        setMeasuredDimension(width, height)
    }

    private fun calculateCxOffset(desiredWidth: Int, width: Int) {
        if (width < desiredWidth) {
            cxOffset = 0
            return
        }

        cxOffset = (width - desiredWidth) / 2 + outlinedCirclePaint.strokeWidth.toInt()
    }

    override fun onDraw(canvas: Canvas?) {
        if (passwordLength == 0 || canvas == null) return

        repeat(passwordLength) { i ->
            val cx = scrollX + (i * circleDiameter) + (charMargin * i) + circleRadius
            val isLastChar = i == (getTextLength() - 1)

            when {
                showPassword -> drawPasswordNumber(canvas, cx, text.toString(), i)

                (isLastChar && !hideLasChar) -> drawPasswordNumber(canvas, cx, text.toString(), i)

                (getTextLength() > i) -> drawFilledCircle(canvas, cx)

                else -> drawOutlinedCircle(canvas, cx)
            }
        }
    }

    private fun drawFilledCircle(canvas: Canvas, cx: Float) {
        canvas.drawCircle(
            cx + cxOffset,
            (bottom - top) / 2f + scrollY,
            circleRadius,
            filledCirclePaint
        )
    }

    private fun drawOutlinedCircle(canvas: Canvas, cx: Float) {
        canvas.drawCircle(
            cx + cxOffset,
            (bottom - top) / 2f + scrollY, circleRadius, outlinedCirclePaint
        )
    }

    private fun drawPasswordNumber(canvas: Canvas, cx: Float, text: String, index: Int) {
        val passwordNumberBounds = Rect()

        passwordTextPaint.getTextBounds(
            text, index, index + 1, passwordNumberBounds
        )

        canvas.drawText(
            text[index].toString(),
            cx + cxOffset - passwordNumberBounds.exactCenterX(),
            (bottom - top) / 2f - passwordNumberBounds.exactCenterY() + scrollY,
            passwordTextPaint
        )
    }

    companion object {
        const val CHAR_VISIBILITY_TIME = 600L
    }
}
