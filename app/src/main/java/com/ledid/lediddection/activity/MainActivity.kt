package com.ledid.lediddection.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ledid.lediddection.R
import com.ledid.lediddection.base.BaseActivity
import com.ledid.lediddection.base.DialogAlertIOS
import com.ledid.lediddection.base.TypeFee
import com.ledid.lediddection.core.Camera2
import com.ledid.lediddection.databinding.ActivityMainBinding
import com.ledid.lediddection.extensions.setSingleClickListener
import com.ledid.lediddection.utils.Constants
import io.reactivex.disposables.Disposable

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun getLayoutRes() = R.layout.activity_main
    private var dialogAlertIOS: DialogAlertIOS? = null

    private lateinit var camera2: Camera2
    private var disposable: Disposable? = null
    private var nPixel = 8

    override fun onStart() {
        super.onStart()
        checkCameraPermission()
    }

    override fun setup() {
        dialogAlertIOS = DialogAlertIOS(
            this,
            title = getString(R.string.ledid_title_setup_camera, "LedID"),
            msg = getString(R.string.ledid_message_setup_camera),
            titleYes = getString(R.string.settings_title),
            titleNo = getString(R.string.btn_cancel),
        ) {
            when (it) {
                TypeFee.ACCEPT -> {
                    checkCameraPermission()
                }
                TypeFee.REJECT -> {
                    binding.tvError.visibility = View.VISIBLE
                }
                else -> {

                }
            }
        }
        binding.btnCamera.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() = when {
        ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED -> {
            dialogAlertIOS?.dismiss()
            binding.tvError.visibility = View.GONE
            binding.btnCamera.visibility = View.GONE
            binding.viewCamera.visibility = View.VISIBLE
            initCamera2Api()

        }
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
            dialogAlertIOS?.show()
        }
        else -> {
            requestCameraPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    dialogAlertIOS?.dismiss()
                    binding.tvError.visibility = View.GONE
                    binding.btnCamera.visibility = View.GONE
                    binding.viewCamera.visibility = View.VISIBLE
                    initCamera2Api()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.CAMERA
                        )
                    ) {
                        dialogAlertIOS?.show()
                    } else {
                        requestCameraPermission()
                    }
                }
                return
            }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun initCamera2Api() {
        val packageManager = this.packageManager
        val hasCamera2 = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)


        if (hasCamera2) {
            binding.apply {
                camera2 = Camera2(this@MainActivity, cameraView, this)
                tvValuePixel.text = camera2.getNPixel().toString()
                tvThresholdMin.text = Constants.minThreshold.toString()
                tvThresholdMax.text = Constants.maxThreshold.toString()
                btnMinus.setOnClickListener {
                    camera2.updateNumberPixel(--nPixel)
                    tvValuePixel.text = camera2.getNPixel().toString()
                }
                btnAdd.setOnClickListener {
                    camera2.updateNumberPixel(++nPixel)
                    tvValuePixel.text = camera2.getNPixel().toString()
                }
                sliderThreshold.run {
                    valueFrom = Constants.minThreshold.toFloat()
                    valueTo = Constants.maxThreshold.toFloat()
                    stepSize = 1f
                    value = Constants.maxThreshold.toFloat() / 2
                    tvValue.text = "Threshold : ${value.toInt()}"
                    addOnChangeListener { _, value, _ ->
                        tvValue.text = "Threshold : ${value.toInt()}"
                        camera2.updateThreshold(value.toInt())
                    }
                }
                ivRotateCamera.setOnClickListener {
                    camera2.switchCamera()
                }
                ivCaptureImage.setSingleClickListener {
                    camera2.isPicture = true
                }
            }
        } else {
            showToast("lỗi cam")
        }
    }


    override fun onPause() {
        if (::camera2.isInitialized) {
            camera2.close()
        }
        super.onPause()
    }

    override fun onResume() {
        if (::camera2.isInitialized) {
            camera2.onResume()
        }
        super.onResume()
    }

    override fun onDestroy() {
        if (disposable != null)
            disposable!!.dispose()
        super.onDestroy()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}