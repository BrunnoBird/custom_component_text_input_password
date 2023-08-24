package com.bgaprojects.custom_component_textfield_password.component

import android.content.Context
import android.content.res.TypedArray
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatEditText
import com.bgaprojects.custom_component_textfield_password.R

class PasswordInput @JvmOverloads constructor(
    @NonNull context: Context,
    @Nullable attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.PasswordInput
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val passwordEditText: EditTextPassword by lazy {
        findViewById(R.id.editText)
    }

    var isError = false
        set(value) {
            field = value
            passwordEditText.setText("")
        }

    init {
        setup(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.PasswordInput,
                defStyleAttr,
                defStyleRes
            )
        )
    }

    private fun setup(typedArray: TypedArray) {
        inflate(context, R.layout.password_input, this)

        passwordEditText.passwordLength =
            typedArray.getInteger(R.styleable.PasswordInput_bga_password_length, 6)
    }

    fun addTextChangedListener(watcher: TextWatcher?) {
        passwordEditText.addTextChangedListener(watcher)
    }
}
