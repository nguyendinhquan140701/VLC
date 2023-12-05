package com.ledid.lediddection.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.ledid.lediddection.databinding.DialogAlertIosBinding

enum class TypeFee { ACCEPT, REJECT, DISCUSS, SKIP }
class DialogAlertIOS(
    private val mContext: Context,
    private val title: String = "",
    private val msg: String = "",
    private val titleYes: String = "",
    private val titleNo: String = "",
    private val onClick: (type: TypeFee) -> Unit
) : Dialog(mContext) {
    private lateinit var binding: DialogAlertIosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAlertIosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.attributes?.apply {
            gravity = Gravity.CENTER
            height = WindowManager.LayoutParams.MATCH_PARENT
            width = WindowManager.LayoutParams.MATCH_PARENT
        }
        init()
        addListener()
    }

    private fun init() {
        binding.apply {
            tvTitle.text = title
            tvMsg.text = msg
            btnYes.text = titleYes
            btnNo.text = titleNo
        }
    }

    private fun addListener() {
        binding.apply {
            btnNo.setOnClickListener {
                onClick.invoke(TypeFee.REJECT)
                dismiss()
            }
            btnYes.setOnClickListener {
                onClick.invoke(TypeFee.ACCEPT)
                dismiss()
            }
        }
    }
}