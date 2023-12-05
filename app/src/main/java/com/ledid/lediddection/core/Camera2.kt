package com.ledid.lediddection.core

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.Matrix.ScaleToFit
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
//import com.chaquo.python.Python
import com.ledid.lediddection.R
import com.ledid.lediddection.databinding.ActivityMainBinding
import com.ledid.lediddection.utils.Converters
import java.util.*
import java.util.Collections.singletonList


class Camera2(
    private val activity: Activity,
    private val textureView: AutoFitTextureView,
    private val binding: ActivityMainBinding
) {
    private var onBitmapReady: (Bitmap) -> Unit = {}
    private val cameraManager: CameraManager =
        textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var previewSize: Size? = null
    private var cameraId = "-1"
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var captureRequest: CaptureRequest? = null
    private var cameraState = STATE_PREVIEW
    private var surface: Surface? = null
    private var isFlashSupported = true
    private var mSensorOrientation = 0
    private var thresholdDefault = 115
    private var nPixel = 8


//    private val py: Python = Python.getInstance()

    //private val processFrame = py.getModule("process_frame2")
//    private val processFrame = py.getModule("test_2_RoI")
//    private val processFrame = py.getModule("2Led_commented")
//    private val processFrame = py.getModule("4Led")
//    private val processFrame = py.getModule("4Led_editing")
//

    private var fpsCallback: Choreographer.FrameCallback? = null
    private var frameCount = 0
    private var startTimeMillis: Long = 0

    private var fpsMax = 30
    private var isFirst = true

    private var arrCode1 = arrayListOf<String>()
    private var arrCode2 = arrayListOf<String>()
    private var arrCode3 = arrayListOf<String>()
    private var arrCode4 = arrayListOf<String>()


    private var arrCode1Test = arrayListOf<String>()
    private var arrCode2Test = arrayListOf<String>()
    private var arrCode3Test = arrayListOf<String>()
    private var arrCode4Test = arrayListOf<String>()

    private val defautlCount = 1000
    private var countImage = defautlCount
    var isPicture = false


    private val cameraCaptureCallBack = object : CameraCaptureSession.CaptureCallback() {
        private fun process() {
            when (cameraState) {
                STATE_PREVIEW -> {

                }

                STATE_WAITING_LOCK -> {
                    captureStillPicture()
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult
        ) {
            process()
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
        ) {
            process()
        }
    }

    private companion object {

        private const val STATE_PREVIEW = 0

        private const val STATE_WAITING_LOCK = 1

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private const val MAX_PREVIEW_WIDTH = 1920

        private const val MAX_PREVIEW_HEIGHT = 1080

        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {
            val bigEnough = arrayListOf<Size>()
            val notBigEnough = arrayListOf<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }
            return when {
                bigEnough.isNotEmpty() -> Collections.min(bigEnough, compareSizesByArea)
                notBigEnough.isNotEmpty() -> Collections.max(notBigEnough, compareSizesByArea)
                else -> {
                    Log.e("Camera", "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }

        private val compareSizesByArea = Comparator<Size> { lhs, rhs ->
            java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
            startFPSCounter()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            stopFPSCounter()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            updateVIew() {
                isFirst = false
            }

            binding.apply {
                val bitmap = cameraView.bitmap
                bitmap?.let {
                    if (isPicture) {
                        if (countImage > 0) {
                            Toast.makeText(
                                activity,
                                "Saving Picture $countImage",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Converters.convertBitmapToFile(bitmap) {}
                            --countImage;
                        } else {
                            countImage = defautlCount;
                            isPicture = false
                        }
                        binding.ivCaptureImage.isEnabled = false
                    } else {
                        binding.ivCaptureImage.isEnabled = true
                    }
                }
            }

        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@Camera2.cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            this@Camera2.cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
        }
    }

    fun updateThreshold(count: Int = 30) {
        this.thresholdDefault = count
    }

    fun updateNumberPixel(count: Int = 8) {
        this.nPixel = count
        if (count <= 4) {
            this.nPixel = 4
        } else if (count >= 15) {
            this.nPixel = 15
        }
    }

    fun getNPixel() = this.nPixel

    fun countMostFrequentStrings(strings: ArrayList<String>): Pair<String, Int> {
        val frequencyMap = HashMap<String, Int>()

        for (string in strings) {
            frequencyMap[string] = frequencyMap.getOrDefault(string, 0) + 1
        }

        var mostFrequentString = ""
        var maxFrequency = 0

        for ((string, frequency) in frequencyMap) {
            if (frequency > maxFrequency) {
                mostFrequentString = string
                maxFrequency = frequency
            }
        }

        return Pair(mostFrequentString, maxFrequency)
    }

    fun countCode(strings: ArrayList<String>): HashMap<String, Int> {
        val frequencyMap = HashMap<String, Int>()

        for (string in strings) {
            frequencyMap[string] = frequencyMap.getOrDefault(string, 0) + 1
        }

        return frequencyMap
    }


    private fun startFPSCounter() {
        frameCount = 0
        startTimeMillis = System.currentTimeMillis()
        fpsCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
                if (elapsedTimeMillis >= 1000) {
                    val fps = (frameCount * 1000 / elapsedTimeMillis).toFloat()
                    binding.tvFps.post {
                        binding.tvFps.text =
                            if (fps.toInt() >= fpsMax) activity.getString(
                                R.string.txt_fps,
                                fpsMax.toString()
                            )
                            else activity.getString(R.string.txt_fps, fps.toInt().toString())
                    }
                    frameCount = 0
                    startTimeMillis = System.currentTimeMillis()
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(fpsCallback)
    }

    private fun stopFPSCounter() {
        fpsCallback?.let {
            Choreographer.getInstance().removeFrameCallback(it)
            fpsCallback = null
        }
    }

    fun onResume() {
        openBackgroundThread()
        isFirst = true
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else textureView.surfaceTextureListener = surfaceTextureListener
    }

    fun close() {
        closeCamera()
        closeBackgroundThread()
    }

    private fun closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession!!.close()
            cameraCaptureSession = null
            //   cameraSessionClosed = true
        }
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread!!.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        }
    }

    private fun openCamera(width: Int, height: Int) {
        if (ContextCompat.checkSelfPermission(
                textureView.context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
//            binding.viewImage.maxHeight = height
            setUpCameraOutputs(width, height)
            configureTransform(width, height)
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        } else Log.e("Camera2", "Requires Camera Permission")
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

                if (cameraFacing == this.cameraFacing) {
                    val streamConfigurationMap = cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )
                    val largest = Collections.max(
                        streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)?.toList(),
                        compareSizesByArea
                    )
                    val displayRotation = activity.windowManager.defaultDisplay.rotation

                    mSensorOrientation =
                        cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0

                    val aeFpsRanges =
                        cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    fpsMax = aeFpsRanges?.last()?.upper ?: 30
                    val exposureTime =
                        cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                    Log.d("tyhoang", "setUpCameraOutputs: ${exposureTime?.lower}")
                    Log.d("tyhoang", "setUpCameraOutputs: ${exposureTime?.upper}")
                    var swappedDimensions = false

                    when (displayRotation) {
                        Surface.ROTATION_0 -> {
                        }

                        Surface.ROTATION_90 -> {
                        }

                        Surface.ROTATION_180 -> {
                            swappedDimensions =
                                mSensorOrientation == 90 || mSensorOrientation == 270
                        }

                        Surface.ROTATION_270 -> {
                            swappedDimensions = mSensorOrientation == 0 || mSensorOrientation == 180
                        }

                        else -> Log.e("Camera2", "Display rotation is invalid: $displayRotation")

                    }

                    val displaySize = Point()

                    activity.windowManager.defaultDisplay.getSize(displaySize)

                    var rotatedPreviewWidth = width
                    var rotatedPreviewHeight = height
                    var maxPreviewWidth = displaySize.x
                    var maxPreviewHeight = displaySize.y

                    if (swappedDimensions) {
                        rotatedPreviewWidth = height
                        rotatedPreviewHeight = width
                        maxPreviewWidth = displaySize.y
                        maxPreviewHeight = displaySize.x
                    }

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT
                    }
                    previewSize = chooseOptimalSize(
                        streamConfigurationMap!!.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        maxPreviewWidth,
                        maxPreviewHeight,
                        largest
                    )

                    val orientation = activity.resources.configuration.orientation

                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        textureView.setAspectRatio(
                            previewSize!!.width, previewSize!!.height
                        )
                    } else {
                        textureView.setAspectRatio(
                            previewSize!!.height, previewSize!!.width
                        )
                    }
                    updateVIew() {}
                    val flashSupported =
                        cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    isFlashSupported = flashSupported == null ?: false

                    this.cameraId = cameraId
                    return
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun updateVIew(lock: (Boolean) -> Unit) {
        if (isFirst) {
            val layoutParams = binding.linearLayout2.layoutParams
            layoutParams.height =
                binding.bgMain.height - binding.bgMain.width * previewSize!!.width / previewSize!!.height
            binding.linearLayout2.layoutParams = layoutParams
        }
        lock
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect =
            RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun getOrientation(rotation: Int) =
        (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360

    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("camera_background_thread")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    fun createPreviewSession() {
        try {
            val surfaceTexture = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            if (surface == null) surface = Surface(surfaceTexture)

            val previewSurface = surface

            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(previewSurface!!)

            cameraDevice!!.createCaptureSession(
                singletonList(previewSurface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            return
                        }

                        try {
                            this@Camera2.cameraCaptureSession = cameraCaptureSession
//                            val fpsRange: Range<Int> = Range(10000, 30000)
//                            captureRequestBuilder!!.set(
//                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange
//                            )
                            captureRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF
                            )
                            captureRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            captureRequest = captureRequestBuilder!!.build()

                            this@Camera2.cameraCaptureSession!!.setRepeatingRequest(
                                captureRequest!!, cameraCaptureCallBack, backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                }, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun lockPreview() {
        try {
            captureRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            cameraState = STATE_WAITING_LOCK
            cameraCaptureSession!!.capture(
                captureRequestBuilder!!.build(), cameraCaptureCallBack, backgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unlockPreview() {
        try {
            captureRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )

            cameraCaptureSession!!.capture(
                captureRequestBuilder!!.build(), cameraCaptureCallBack, backgroundHandler
            )
            cameraState = STATE_PREVIEW

            cameraCaptureSession!!.setRepeatingRequest(
                captureRequest!!, cameraCaptureCallBack, backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun switchCamera() {
        close()
        if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
            cameraFacing = CameraCharacteristics.LENS_FACING_FRONT
        } else {
            cameraFacing = CameraCharacteristics.LENS_FACING_BACK
        }
        onResume()
    }

    fun switchCamera(view: View) {
        val rotationHolder = PropertyValuesHolder.ofFloat(View.ROTATION, 0f, 360f)
        val alphaHolder = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(view, rotationHolder, alphaHolder)
        animator.duration = 500

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                close()
                cameraFacing =
                    if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) CameraCharacteristics.LENS_FACING_FRONT
                    else CameraCharacteristics.LENS_FACING_BACK
                onResume()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}

        })

        // Start the animation
        animator.start()
    }

    private fun captureBitmap() {
        if (textureView.isAvailable) {
            onBitmapReady(binding.bgMain.getBitmapView())
        }
    }

    private fun View.getBitmapView(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            this.width,
            this.height - binding.ivRotateCamera.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        return bitmap
    }

    private fun captureStillPicture() {
        try {
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(surface!!)
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            )
            captureBuilder.set(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF
            )

            val rotation = activity.windowManager.defaultDisplay.rotation

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))

            cameraCaptureSession!!.stopRepeating()
            cameraCaptureSession!!.abortCaptures()
            cameraCaptureSession!!.capture(
                captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        captureBitmap()
                        unlockPreview()
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun takePhoto(onBitmapReady: (Bitmap) -> Unit) {
        this.onBitmapReady = onBitmapReady
        lockPreview()
    }
}






