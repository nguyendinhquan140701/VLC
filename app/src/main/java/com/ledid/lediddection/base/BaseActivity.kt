package com.ledid.lediddection.base

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBinding
import com.ledid.lediddection.R
import java.lang.reflect.ParameterizedType

@Suppress("UNCHECKED_CAST")
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    open lateinit var binding: VB
    private var mProgressDialog: ProgressDialog? = null
    @LayoutRes
    abstract fun getLayoutRes(): Int
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setCancelable(false)
        setup()
    }

    private fun inflateBinding(layoutInflater: LayoutInflater): VB {
        val viewBindingClass = getViewBindingClass()
        val inflateMethod = viewBindingClass.getMethod("inflate", LayoutInflater::class.java)
        return inflateMethod.invoke(null, layoutInflater) as VB
    }

    private fun getViewBindingClass(): Class<VB> {
        val type = javaClass.genericSuperclass
        val arguments = (type as ParameterizedType).actualTypeArguments
        val argument = arguments[0]
        return argument as Class<VB>
    }

    abstract fun setup()

    fun showLog(msg: String) {
        Log.d(javaClass.simpleName, msg)
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun showAlert() {

    }

    private fun showSetupAlert() {
        openPermissionSetting()
    }

    private fun Activity.openPermissionSetting() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun showLoading(isShow: Boolean) {
        mProgressDialog!!.setMessage(getString(R.string.txt_waiting))
        try {
            if (isShow) {
                mProgressDialog!!.show()
            } else {
                if (mProgressDialog!!.isShowing) {
                    mProgressDialog!!.dismiss()
                }
            }
        } catch (ex: IllegalArgumentException) {
        }

    }

    fun showLoading(message: String) {
        mProgressDialog!!.setMessage(message)
    }
}