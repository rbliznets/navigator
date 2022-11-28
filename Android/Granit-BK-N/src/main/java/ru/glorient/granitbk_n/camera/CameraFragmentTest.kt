package ru.glorient.granitbk_n.camera

import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.media.*
import android.opengl.GLES10
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.accessory.OpenFileDialog
import ru.glorient.granitbk_n.camera.opengl.CanvasDrawable
import ru.glorient.granitbk_n.camera.opengl.helpers.ContextHelper
import ru.glorient.granitbk_n.camera.opengl.helpers.OpenGLExternalTexture
import ru.glorient.granitbk_n.camera.opengl.helpers.OpenGLScene
import ru.glorient.granitbk_n.camera.opengl.machines.*
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraFragmentTest : Fragment(R.layout.fragment_camera_test) {

    // Задняя камера
    private var mBackCamera: Camera? = null

    // Фронтальная камера
    private var mFrontCamera: Camera? = null

    // Первью задней камеры
    private var mBackCamPreview: BackCamera? = null
    // Первью фронтальной камеры

    // Флаги для записи
    private var flagButtonRecFront = false
    private var flagButtonRecBack = false

    private var mediaRecorderFront: MediaRecorder? = MediaRecorder()
    private var mediaRecorderBack: MediaRecorder? = MediaRecorder()

    private var flagCycleCameraFront: Boolean = true
    private var flagCycleCameraBack: Boolean = true
//    val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var recordBtn: TextView
    private lateinit var recordBtn2: TextView

//    private lateinit var glSurfaceView: GLSurfaceView
//    private val glSurfaceMachine = GLSurfaceMachine(1)
//    private val cameraMachine = CameraMachine(1)
//    private val encoderMachine = EncoderMachine(1)
//
//
//    private lateinit var glSurfaceView2: GLSurfaceView
//    private val glSurfaceMachine2 = GLSurfaceMachine(0)
//    private val cameraMachine2 = CameraMachine(0)
//    private val encoderMachine2 = EncoderMachine(0)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        val view = this
//            .layoutInflater
//            .inflate(R.layout.activity_camera, null)
        Log.e(LOG_TAG, "#############m onViewCreated")

//        mButtonRecFrontCamera = view.findViewById(R.id.buttonRecFront)
//        mTextureViewFront = view.findViewById(R.id.textureViewFront)
        progressBarFront = view.findViewById(R.id.progressBarFront)
        imgRecFront = view.findViewById(R.id.imgRecFront)
        etTimeSizeFront = view.findViewById(R.id.etTimeSize)
//        etMemorySizeFront = view.findViewById(R.id.etMemorySize)
        buttonPlayFront = view.findViewById(R.id.buttonPlayFront)

//        mButtonRecBackCamera = view.findViewById(R.id.buttonRecBack)
//        mTextureViewBack = view.findViewById(R.id.textureViewBack)
        progressBarBack = view.findViewById(R.id.progressBarBack)
        imgRecBack = view.findViewById(R.id.imgRecBack)
        etTimeSizeBack = view.findViewById(R.id.etTimeSizeBack)
//        etMemorySizeBack = view.findViewById(R.id.etMemorySizeBack)
        buttonPlayBack = view.findViewById(R.id.buttonPlayBack)

//        mButtonRecFrontCamera.setOnClickListener(recordVideoListenerFront)
//        mButtonRecBackCamera.setOnClickListener(recordVideoListenerBack)



        // Воспроизводим видео Front
        buttonPlayFront.setOnClickListener {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return@setOnClickListener

            // Проверяем есть ли SD карта
            val listOfInternalAndExternalStorage = requireContext().getExternalFilesDirs(null)
            val filePathFolder = if (listOfInternalAndExternalStorage.size >= 2) {
                requireContext().getExternalFilesDirs("/Front")?.get(1)?.path.orEmpty()
            } else {
                requireContext().getExternalFilesDir("/Front")?.path.orEmpty()
            }
            val fileDialog = OpenFileDialog(requireContext(), filePathFolder)
            val drawFolder = resources.getDrawable(R.drawable.ic_folder)
            fileDialog.setFolderIcon(drawFolder)
            fileDialog.setOpenDialogListener { fileName ->
                onClickPlayFront(fileName)
            }

            val dialog = fileDialog.show()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        // Воспроизводим видео Back
        buttonPlayBack.setOnClickListener {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return@setOnClickListener

            // Проверяем есть ли SD карта
            val listOfInternalAndExternalStorage = requireContext().getExternalFilesDirs(null)
            val filePathFolder = if (listOfInternalAndExternalStorage.size >= 2) {
                requireContext().getExternalFilesDirs("/Back")?.get(1)?.path.orEmpty()
            } else {
                requireContext().getExternalFilesDir("/Back")?.path.orEmpty()
            }

            val fileDialog = OpenFileDialog(requireContext(), filePathFolder)
            val drawFolder = resources.getDrawable(R.drawable.ic_folder)
            fileDialog.setFolderIcon(drawFolder)
            fileDialog.setOpenDialogListener { fileName ->
                onClickPlayBack(fileName)
            }

            val dialog = fileDialog.show()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

//    // Обработчик нажатия на кнопку Запись с задней камеры
//    private val recordVideoListenerFront = View.OnClickListener {
//        if (flagButtonRecFront) {
//            mediaRecorderFront?.stop()
//            mediaRecorderFront?.reset()
//            mFrontCamera!!.lock()
//            flagButtonRecFront = false
//            mButtonRecFrontCamera.text = "Начать запись Front"
//            imgRecFront.visibility = View.INVISIBLE
//
////            scope.launch {
//            restartSurfaceFront()
////            }
//        } else {
//            prepareToRecordVideoFront()
//            try {
//                mediaRecorderFront?.prepare()
//            } catch (e: IOException) {
//                Log.i(LOG_TAG, "Prepare failed")
//            }
//
//            mediaRecorderFront?.start()
//
//            flagButtonRecFront = true
//            imgRecFront.visibility = View.VISIBLE
//            mButtonRecFrontCamera.text = "Остановить запись Front"
//        }
//    }
//
//    // Обработчик нажатия на кнопку Запись с фронтальной камеры
//    private val recordVideoListenerBack = View.OnClickListener {
//        if (flagButtonRecBack) {
//            mediaRecorderBack?.stop()
//            mediaRecorderBack?.reset()
//            mBackCamera!!.lock()
//            flagButtonRecBack = false
//            mButtonRecBackCamera.text = "Начать запись Back"
//            imgRecBack.visibility = View.INVISIBLE
////            scope.launch {
//            restartSurfaceBack()
////            }
//        } else {
//            prepareToRecordVideoBack()
//            try {
//                mediaRecorderBack?.prepare()
//            } catch (e: IOException) {
//                Log.i(LOG_TAG, "Prepare failed")
//            }
//            mediaRecorderBack?.start()
//            flagButtonRecBack = true
//            imgRecBack.visibility = View.VISIBLE
//            mButtonRecBackCamera.text = "Остановить запись Back"
//        }
//    }

    // Подготавливаемся к записи с задней камеры
    private fun prepareToRecordVideoFront() {
//        mFrontCamera!!.unlock()
        mediaRecorderFront = MediaRecorder()
        mediaRecorderFront?.setCamera(mFrontCamera)

        val cp = CamcorderProfile.get(1, CamcorderProfile.QUALITY_1080P)

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
//        mediaRecorderFront?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mediaRecorderFront?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorderFront?.setOutputFormat(cp.fileFormat)
        mediaRecorderFront?.setVideoEncoder(cp.videoCodec)
        mediaRecorderFront?.setVideoEncodingBitRate(cp.videoBitRate)
        mediaRecorderFront?.setVideoFrameRate(cp.videoFrameRate)
        mediaRecorderFront?.setVideoSize(cp.videoFrameWidth, cp.videoFrameHeight)

        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return
        // Проверяем есть ли SD карта
        val listOfInternalAndExternalStorage = requireContext().getExternalFilesDirs(null)
        val filePathFolder = if (listOfInternalAndExternalStorage.size >= 2) {
            requireContext().getExternalFilesDirs("/Front")?.get(1)?.path.orEmpty()
        } else {
            requireContext().getExternalFilesDir("/Front")?.path.orEmpty()
        }
        // Указываем путь к папке и называем файл
        val folder = File(filePathFolder)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HH.mm.ss").format(Date())
        mCurrentFileFront = File(folder, "camFront$timeStamp.mp4")

        mediaRecorderFront?.setOutputFile(mCurrentFileFront.absolutePath)

        mediaRecorderFront?.setPreviewDisplay(texture.surface)
//        mediaRecorderFront?.setPreviewDisplay(mFrontCamPreviewCanvas?.holder?.surface)

        val maxDuration = (etTimeSizeFront.text.toString().toInt()) * 60 * 1000
        val maxFileSize = (etMemorySizeFront.text.toString().toLong()) * 1024 * 1024
        mediaRecorderFront?.setMaxDuration(maxDuration)
        mediaRecorderFront?.setMaxFileSize(maxFileSize)

        var i = 0
        mediaRecorderFront?.setOnInfoListener { mr, what, extra ->
            Log.d(LOG_TAG, "Back onInfo mr = $mr, what = $what, extra = $extra")
            try {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    if (i == 0) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
                            Log.d(
                                LOG_TAG,
                                "Back Ограничение по времени записи - $maxDuration милисек"
                            )
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                            Log.d(
                                LOG_TAG,
                                "Back Ограничение по объему записи - $maxFileSize байт"
                            )


//                            if ((myCameras[CAMERA2] != null) and (mMediaRecorderBack != null)) {
//                                myCameras[CAMERA2]?.stopRecordingVideo(CAMERA2, mMediaRecorderBack)
                        if ((mFrontCamera != null) and (mediaRecorderFront != null)) {
                            mediaRecorderFront?.stop()
                            mediaRecorderFront?.release()

                            prepareToRecordVideoFront()
                            try {
                                mediaRecorderFront?.prepare()
                            } catch (e: IOException) {
                                Log.i(LOG_TAG, "Prepare failed")
                            }
                            mediaRecorderFront?.start()
//                                Handler(Looper.getMainLooper()).postAtTime({
//                                    Log.d(LOG_TAG, "Handler")
//                                }, SystemClock.uptimeMillis() + 100L)
                        }
                    }
                    i++
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Exception = $e")
            }
        }

        mediaRecorderFront?.setOnErrorListener { mediaRecorder, i, i1 ->
            Log.i(
                LOG_TAG,
                "RECORDING FAILED ERROR CODE: $i AND EXTRA CODE: $i1"
            )
        }
    }

    // Подготавливаемся к записи с фронтальной камеры
    private fun prepareToRecordVideoBack() {
        mBackCamera!!.unlock()
        mediaRecorderBack = MediaRecorder()
        mediaRecorderBack?.setCamera(mBackCamera)

        val cp = CamcorderProfile.get(0, CamcorderProfile.QUALITY_1080P)

        mediaRecorderBack?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mediaRecorderBack?.setOutputFormat(cp.fileFormat)
        mediaRecorderBack?.setVideoEncoder(cp.videoCodec)
        mediaRecorderBack?.setVideoEncodingBitRate(cp.videoBitRate)
        mediaRecorderBack?.setVideoFrameRate(cp.videoFrameRate)
        mediaRecorderBack?.setVideoSize(cp.videoFrameWidth, cp.videoFrameHeight)

        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return

        // Проверяем есть ли SD карта
        val listOfInternalAndExternalStorage = requireContext().getExternalFilesDirs(null)
        val filePathFolder = if (listOfInternalAndExternalStorage.size >= 2) {
            requireContext().getExternalFilesDirs("/Back")?.get(1)?.path.orEmpty()
        } else {
            requireContext().getExternalFilesDir("/Back")?.path.orEmpty()
        }
        // Указываем путь к папке и называем файл
        val folder = File(filePathFolder)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HH.mm.ss").format(Date())
        mCurrentFileBack = File(folder, "camBack$timeStamp.mp4")

        mediaRecorderBack?.setOutputFile(mCurrentFileBack.absolutePath)
//        mediaRecorderBack?.setPreviewDisplay(mBackCamPreview?.holder?.surface)
        mediaRecorderBack?.setPreviewDisplay(mBackCamPreview?.holder?.surface)

        val maxDuration = (etTimeSizeBack.text.toString().toInt()) * 60 * 1000
        val maxFileSize = (etMemorySizeBack.text.toString().toLong()) * 1024 * 1024
        mediaRecorderBack?.setMaxDuration(maxDuration)
        mediaRecorderBack?.setMaxFileSize(maxFileSize)

        var i = 0
        mediaRecorderBack?.setOnInfoListener { mr, what, extra ->
            Log.d(LOG_TAG, "Back onInfo mr = $mr, what = $what, extra = $extra")
            try {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    if (i == 0) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
                            Log.d(
                                LOG_TAG,
                                "Back Ограничение по времени записи - $maxDuration милисек"
                            )
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                            Log.d(
                                LOG_TAG,
                                "Back Ограничение по объему записи - $maxFileSize байт"
                            )


//                            if ((myCameras[CAMERA2] != null) and (mMediaRecorderBack != null)) {
//                                myCameras[CAMERA2]?.stopRecordingVideo(CAMERA2, mMediaRecorderBack)
                        if ((mBackCamera != null) and (mediaRecorderBack != null)) {
                            mediaRecorderBack?.stop()
                            mediaRecorderBack?.release()

                            prepareToRecordVideoBack()
                            try {
                                mediaRecorderBack?.prepare()
                            } catch (e: IOException) {
                                Log.i(LOG_TAG, "Prepare failed")
                            }
                            mediaRecorderBack?.start()
//                                Handler(Looper.getMainLooper()).postAtTime({
//                                    Log.d(LOG_TAG, "Handler")
//                            mMediaRecorderBack.start()
//                                }, SystemClock.uptimeMillis() + 100L)
                        }
                    }
                    i++
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Exception = $e")
            }
        }
    }

    // Захватываем изображение задней камеры
    private fun restartSurfaceFront() {
//        withContext(Dispatchers.Main) {
//        CoroutineScope(Dispatchers.IO).launch {
//        camera = Camera.open(1)
//        val parms: Camera.Parameters? = camera?.getParameters()
//        parms?.setRecordingHint(true)
//        camera?.setParameters(parms)
//
//        try {
//            camera?.setPreviewTexture(OpenGLScene())
//        } catch (ioe: IOException) {
//            throw RuntimeException(ioe)
//        }
//        camera?.startPreview()


//        mFrontCamera = getCameraInstance(1)
//        Log.d(LOG_TAG, "mFrontCamera $mFrontCamera")
//
//        if (mFrontCamera == null) {
//            return
////                return@withContext
//        } else {
//            flagCycleCameraFront = false
//            progressBarFront.visibility = View.INVISIBLE
//        }
////        val parms: Camera.Parameters = mFrontCamera!!.getParameters()
////        parms.setRecordingHint(true)
//
////        val frontPreview = activity?.findViewById<View>(R.id.textureViewFront) as FrameLayout
//
//
////        mGLSurfaceView = TouchSurfaceView(requireContext())
////        mGLSurfaceView!!.setZOrderOnTop(false)
////        mGLSurfaceView!!.setZOrderMediaOverlay(false)
////        mGLSurfaceView!!.holder.setFormat(PixelFormat.TRANSPARENT)
////        frontPreview.addView(
////            mGLSurfaceView/*,
////            ViewGroup.LayoutParams(
////                ViewGroup.LayoutParams.FILL_PARENT,
////                ViewGroup.LayoutParams.FILL_PARENT
////            )*/
////        )
//
//        mFrontCamPreview =
////        mFrontCamPreview = FrontCamera(activity, mFrontCamera)
////        mFrontCamPreview!!.holder.setFormat(PixelFormat.TRANSLUCENT)
////        mFrontCamPreview!!.setZOrderOnTop(true)
////        mFrontCamPreview!!.setZOrderMediaOverlay(true)
////        frontPreview.addView(mFrontCamPreview)
//
//            try {
//                mFrontCamera!!.setPreviewTexture(texture.surfaceTexture)
////            mFrontCamera!!.setPreviewDisplay(mFrontCamPreview!!.holder)
//                mFrontCamera!!.startPreview()
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
    }

    // Захватываем изображение фронтальной камеры
    private fun restartSurfaceBack() {
//        CoroutineScope(Dispatchers.IO).launch {
        mBackCamera = getCameraInstance(0)
        Log.d(tag, "mBackCamera $mBackCamera")

        if (mBackCamera == null) {
            return
            //                return@withContext
        } else {
            flagCycleCameraBack = false
            progressBarBack.visibility = View.INVISIBLE
        }

//        mBackCamPreview = BackCamera(activity, mBackCamera)
//        val backPreview = activity?.findViewById<View>(R.id.textureViewBack) as FrameLayout
//        backPreview.addView(mBackCamPreview)
        try {
            mBackCamera!!.setPreviewDisplay(mBackCamPreview!!.getHolder())
            mBackCamera!!.startPreview()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
//        }
    }

    override fun onResume() {
        super.onResume()
        progressBarFront.visibility = View.VISIBLE
        progressBarBack.visibility = View.VISIBLE
        flagCycleCameraFront = true
        flagCycleCameraBack = true
        Log.e(LOG_TAG, "#############m onResume")



////        scope.launch {
//        while (flagCycleCameraFront) {
//            Log.e(LOG_TAG, "#############m onResume flagCycleCamera1 $flagCycleCameraFront")
//            // Захватываем изображение
//            restartSurfaceFront()
//        }
//        while (flagCycleCameraBack) {
//            Log.e(LOG_TAG, "#############m onResume flagCycleCamera2 $flagCycleCameraBack")
//            // Захватываем изображение
//            restartSurfaceBack()
//        }
////        }
    }

    // Останавливаем камеру и освобождаем ее
    override fun onPause() {
        super.onPause()
        Log.e(LOG_TAG, "#############m onPause")
        releaseMediaRecorder()
        if (mBackCamera != null) {
            Log.e(LOG_TAG, "##########mBackCamera release")
            mBackCamera!!.release()
        }
        mBackCamera = null
        if (mFrontCamera != null) {
            Log.e(LOG_TAG, "#############mFrontCamera release")
            mFrontCamera!!.release()
        }
        mFrontCamera = null
    }

    override fun onStop() {
        super.onStop()
        flagCycleCameraFront = false
        flagCycleCameraBack = false

        Log.e(LOG_TAG, "#############m onStop")
    }

//    // Указываем директорию для записи видеофайлов
//    private fun initFile(cameraNamePrefix: String): File? {
//        val currentPath = requireContext()
//            .getExternalFilesDir("/Granit BK-N/Reklama/Записи с камер")?.path.orEmpty()
//        val dir = File(currentPath)
//        if (!dir.exists() && !dir.mkdirs()) {
//            file = null
//        } else {
//            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH.mm.ss").format(Date())
//            val mediaFile: File
//            mediaFile = File(
//                dir.path + File.separator +
//                        cameraNamePrefix + timeStamp + ".mp4"
//            )
//            Log.i(LOG_TAG, mediaFile.absolutePath)
//            return mediaFile
//        }
//        return file
//    }

    // Воспроизводим видео
    private fun onClickPlayFront(path: String) {
        mFrontCamPreview?.visibility = View.GONE
        mFrontCamPreview?.visibility = View.VISIBLE

        try {
            val mediaPlayer = MediaPlayer()
//            mediaPlayer.setDisplay(mFrontCamPreview?.holder)
            mediaPlayer.setSurface(mFrontCamPreview?.holder?.surface)
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
                override fun onPrepared(mp: MediaPlayer?) {
                    mediaPlayer.start()
                }
            })
            mediaPlayer.setOnCompletionListener {
                Log.d(LOG_TAG, "Закончили воспроизведение Front")
                restartSurfaceFront()
            }
//            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // Воспроизводим видео Back
    private fun onClickPlayBack(path: String) {
        mBackCamPreview?.visibility = View.GONE
        mBackCamPreview?.visibility = View.VISIBLE

        try {
            val mediaPlayer = MediaPlayer()
//            mediaPlayer.setDisplay(mFrontCamPreview?.holder)
            mediaPlayer.setSurface(mBackCamPreview?.holder?.surface)
            mediaPlayer.setDataSource(path)
            mediaPlayer.prepare()
            mediaPlayer.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
                override fun onPrepared(mp: MediaPlayer?) {
                    mediaPlayer.start()
                }
            })
            mediaPlayer.setOnCompletionListener {
                Log.d(LOG_TAG, "Закончили воспроизведение Back")
                restartSurfaceBack()
            }
//            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // Закрываем и освобождаем объекты
    private fun releaseMediaRecorder() {
        if (mediaRecorderFront != null) {
            mediaRecorderFront?.reset()
            mediaRecorderFront?.release()
            mediaRecorderFront = null
            if (mFrontCamera != null) mFrontCamera!!.lock()
        }
        if (mediaRecorderBack != null) {
            mediaRecorderBack?.reset()
            mediaRecorderBack?.release()
            mediaRecorderBack = null
            if (mBackCamera != null) mBackCamera!!.lock()
        }
    }

    // Открываем камеры
    private fun getCameraInstance(cameraId: Int): Camera? {
        var camera: Camera? = null
        try {
            camera = Camera.open(cameraId)
        } catch (e: java.lang.Exception) {
            Log.e(
                LOG_TAG,
                "Camera $cameraId is not available $e"
            )
        }
        return camera
    }

    companion object {
        const val LOG_TAG = "Main"
        //        var myCameras = mutableListOf<CameraService?>()
//        lateinit var myCameraFront: CameraService
//        lateinit var myCameraBack: CameraService
//        private lateinit var mCameraManager: CameraManager
//        private lateinit var mCameraManagerBack: CameraManager

        private const val CAMERA1 = 1
        private lateinit var mButtonRecFrontCamera: Button
        private lateinit var imgRecFront: ImageView
        private lateinit var mTextureViewFront: FrameLayout
        private lateinit var progressBarFront: ProgressBar
        private lateinit var etTimeSizeFront: EditText
        private lateinit var etMemorySizeFront: EditText
        private lateinit var buttonPlayFront: ImageButton

        //        lateinit var mMediaRecorderFront: MediaRecorder
        lateinit var mCurrentFileFront: File

        private const val CAMERA2 = 0
        private lateinit var mButtonRecBackCamera: Button
        private lateinit var imgRecBack: ImageView
        private lateinit var mTextureViewBack: FrameLayout
        private lateinit var progressBarBack: ProgressBar
        private lateinit var etTimeSizeBack: EditText
        private lateinit var etMemorySizeBack: EditText
        private lateinit var buttonPlayBack: ImageButton

        lateinit var texture: OpenGLExternalTexture

        //        lateinit var mMediaRecorderBack: MediaRecorder
        lateinit var mCurrentFileBack: File

        //        var mFrontCamPreviewCanvas: SurfaceView? = null
        var mFrontCamPreview: FrontCamera? = null

        var camera: Camera? = null

//        var mBackgroundThread: HandlerThread? = null
//        var mBackgroundHandler: Handler? = null
//
//        var mBackgroundThreadBack: HandlerThread? = null
//        var mBackgroundHandlerBack: Handler? = null
    }
}