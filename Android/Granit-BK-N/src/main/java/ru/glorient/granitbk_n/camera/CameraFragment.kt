package ru.glorient.granitbk_n.camera

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.glorient.granitbk_n.MainActivity
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.accessory.OpenFileDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment(R.layout.fragment_camera) {
    // Задняя камера
    private var mBackCamera: Camera? = null

    // Фронтальная камера
    private var mFrontCamera: Camera? = null

    // Первью задней камеры
    private var mBackCamPreview: BackCamera? = null

    // Первью фронтальной камеры
    private var mFrontCamPreview: FrontCamera? = null

    // Кнопка начать/остановить запись с задней камеры
    private var recordBtn: Button? = null

    // Кнопка начать/остановить запись с фронтальной камеры
    private var recordBtn2: Button? = null

    // Флаги для записи
    private var recording = false
    private var recording2 = false

    private var file: File? = null

    private var mediaRecorder1: MediaRecorder? = MediaRecorder()
    private var mediaRecorder2: MediaRecorder? = MediaRecorder()

    private lateinit var btnStartRecord: Button
    private lateinit var btnStartRecord2: Button
    private lateinit var btnPlay: Button
    private lateinit var videoView: VideoView

    private var flagCycleCamera1: Boolean = true
    private var flagCycleCamera2: Boolean = true
//    val scope = CoroutineScope(Dispatchers.IO)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        val view = this
//            .layoutInflater
//            .inflate(R.layout.activity_camera, null)
        Log.e(MainActivity.TAG, "#############m onViewCreated")

        btnStartRecord = view.findViewById(R.id.btnStartRecord)
        btnStartRecord2 = view.findViewById(R.id.btnStartRecord2)
        btnPlay = view.findViewById(R.id.btnPlay)
        videoView = view.findViewById(R.id.videoView)

        recordBtn = btnStartRecord
        btnStartRecord.setOnClickListener(recordVideoListener1)
        btnStartRecord2.setOnClickListener(recordVideoListener2)
        recordBtn2 = btnStartRecord2

        // Воспроизводим видео
        btnPlay.setOnClickListener {
            val currentPath = requireContext()
                .getExternalFilesDir("/Granit BK-N/Reklama")?.path.orEmpty()
            val fileDialog = OpenFileDialog(requireContext(), currentPath)
            val drawFolder = resources.getDrawable(R.drawable.ic_folder)
            fileDialog.setFolderIcon(drawFolder)
            fileDialog.setOpenDialogListener { fileName ->
                onClickPlay(fileName)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    // Обработчик нажатия на кнопку Запись с задней камеры
    private val recordVideoListener1 = View.OnClickListener {
        if (recording) {
            mediaRecorder1?.stop()
            mediaRecorder1?.reset()
            mBackCamera!!.lock()
            recording = false
            recordBtn!!.text = "Старт 1"

//            scope.launch {
                restartSurface1()
//            }
        } else {
            prepareToRecordVideo()
            try {
                mediaRecorder1?.prepare()
            } catch (e: IOException) {
                Log.i(tag, "Prepare failed")
            }
            mediaRecorder1?.start()
            recording = true
            recordBtn!!.text = "Стоп 1"
        }
    }

    // Обработчик нажатия на кнопку Запись с фронтальной камеры
    private val recordVideoListener2 = View.OnClickListener {
        if (recording2) {
            mediaRecorder2?.stop()
            mediaRecorder2?.reset()
            mFrontCamera!!.lock()
            recording2 = false
            recordBtn2!!.text = "Старт 2"
//            scope.launch {
                restartSurface2()
//            }
        } else {
            prepareToRecordVideo2()
            try {
                mediaRecorder2?.prepare()
            } catch (e: IOException) {
                Log.i(tag, "Prepare failed")
            }
            mediaRecorder2?.start()
            recording2 = true
            recordBtn2!!.text = "Стоп 2"
        }
    }

    // Подготавливаемся к записи с задней камеры
    private fun prepareToRecordVideo() {
        mBackCamera!!.unlock()
        mediaRecorder1 = MediaRecorder()
        mediaRecorder1?.setCamera(mBackCamera)
        mediaRecorder1?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder1?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mediaRecorder1?.setProfile(CamcorderProfile.get(0, CamcorderProfile.QUALITY_1080P))
        mediaRecorder1?.setAudioSamplingRate(8000)
        mediaRecorder1?.setOutputFile(initFile("CAMERA_1_").toString())
        mediaRecorder1?.setPreviewDisplay(mBackCamPreview?.holder?.surface)
        mediaRecorder1?.setOnErrorListener { mediaRecorder, i, i1 ->
            Log.i(
                tag,
                "RECORDING FAILED ERROR CODE: $i AND EXTRA CODE: $i1"
            )
        }
    }

    // Подготавливаемся к записи с фронтальной камеры
    private fun prepareToRecordVideo2() {
        mFrontCamera!!.unlock()
        mediaRecorder2 = MediaRecorder()
        mediaRecorder2?.setCamera(mFrontCamera)

        val cp = CamcorderProfile.get(1, CamcorderProfile.QUALITY_1080P)

        mediaRecorder2?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mediaRecorder2?.setOutputFormat(cp.fileFormat)
        mediaRecorder2?.setVideoEncoder(cp.videoCodec)
        mediaRecorder2?.setVideoEncodingBitRate(cp.videoBitRate)
        mediaRecorder2?.setVideoFrameRate(cp.videoFrameRate)
        mediaRecorder2?.setVideoSize(cp.videoFrameWidth, cp.videoFrameHeight)
        mediaRecorder2?.setOutputFile(initFile("CAMERA_2_").toString())
        mediaRecorder2?.setPreviewDisplay(mFrontCamPreview?.holder?.surface)
    }

    // Захватываем изображение задней камеры
    private fun restartSurface1() {
//        withContext(Dispatchers.Main) {
            mBackCamera = getCameraInstance(0)
            Log.d(MainActivity.TAG, "mBackCamera $mBackCamera")

            if (mBackCamera == null) {
                return
//                return@withContext
            } else {
                flagCycleCamera1 = false
            }

            mBackCamPreview = BackCamera(activity, mBackCamera)
            val backPreview = activity?.findViewById<View>(R.id.surfaceView) as FrameLayout
//        val backPreview = surfaceView as FrameLayout
            backPreview.addView(mBackCamPreview)
            try {
                mBackCamera!!.setPreviewDisplay(mBackCamPreview!!.getHolder())
                mBackCamera!!.startPreview()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
//        }
    }

    // Захватываем изображение фронтальной камеры
    private fun restartSurface2() {
//        withContext(Dispatchers.Main) {
            mFrontCamera = getCameraInstance(1)
            Log.d(MainActivity.TAG, "mFrontCamera $mFrontCamera")

            if (mFrontCamera == null) {
                return
//                return@withContext
            } else {
                flagCycleCamera2 = false
            }

            mFrontCamPreview = FrontCamera(activity, mFrontCamera)
            val frontPreview = activity?.findViewById<View>(R.id.surfaceView2) as FrameLayout
//        val frontPreview = surfaceView2 as FrameLayout
            frontPreview.addView(mFrontCamPreview)
            try {
                mFrontCamera!!.setPreviewDisplay(mFrontCamPreview!!.getHolder())
                mFrontCamera!!.startPreview()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
//        }
    }

    override fun onResume() {
        super.onResume()
        flagCycleCamera1 = true
        flagCycleCamera2 = true
        Log.e(MainActivity.TAG, "#############m onResume")

//        scope.launch {
            while (flagCycleCamera1) {
                Log.e(MainActivity.TAG, "#############m onResume flagCycleCamera1 $flagCycleCamera1")
                // Захватываем изображение
                restartSurface1()
            }
            while (flagCycleCamera2) {
                Log.e(MainActivity.TAG, "#############m onResume flagCycleCamera2 $flagCycleCamera2")
                // Захватываем изображение
                restartSurface2()
            }
//        }
    }

    // Останавливаем камеру и освобождаем ее
    override fun onPause() {
        super.onPause()
        Log.e(MainActivity.TAG, "#############m onPause")
        releaseMediaRecorder()
        if (mBackCamera != null) {
            Log.e(MainActivity.TAG, "##########mBackCamera release")
            mBackCamera!!.release()
        }
        mBackCamera = null
        if (mFrontCamera != null) {
            Log.e(MainActivity.TAG, "#############mFrontCamera release")
            mFrontCamera!!.release()
        }
        mFrontCamera = null
    }

    override fun onStop() {
        super.onStop()
        flagCycleCamera1 = false
        flagCycleCamera2 = false

        Log.e(MainActivity.TAG, "#############m onStop")
    }

    // Указываем директорию для записи видеофайлов
    private fun initFile(cameraNamePrefix: String): File? {
        val currentPath = requireContext()
            .getExternalFilesDir("/Granit BK-N/Reklama/Записи с камер")?.path.orEmpty()
        val dir = File(currentPath)
        if (!dir.exists() && !dir.mkdirs()) {
            file = null
        } else {
            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH.mm.ss").format(Date())
            val mediaFile: File
            mediaFile = File(
                dir.path + File.separator +
                        cameraNamePrefix + timeStamp + ".mp4"
            )
            Log.i(tag, mediaFile.absolutePath)
            return mediaFile
        }
        return file
    }

    // Воспроизводим видео
    private fun onClickPlay(path: String) {
        val mediaController = MediaController(activity)
        mediaController.setMediaPlayer(videoView)
        videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        videoView.setVideoPath(path)
        videoView.setMediaController(mediaController)
        videoView.requestFocus()
        videoView.start()
        mediaController.show()
    }

    // Закрываем и освобождаем объекты
    private fun releaseMediaRecorder() {
        if (mediaRecorder1 != null) {
            mediaRecorder1?.reset()
            mediaRecorder1?.release()
            mediaRecorder1 = null
            if (mBackCamera != null) mBackCamera!!.lock()
        }
        if (mediaRecorder2 != null) {
            mediaRecorder2?.reset()
            mediaRecorder2?.release()
            mediaRecorder2 = null
            if (mFrontCamera != null) mFrontCamera!!.lock()
        }
    }

    // Открываем камеры
    private fun getCameraInstance(cameraId: Int): Camera? {
        var camera: Camera? = null
        try {
            camera = Camera.open(cameraId)
        } catch (e: java.lang.Exception) {
            Log.e(
                tag,
                "Camera $cameraId is not available $e"
            )
        }
        return camera
    }
}