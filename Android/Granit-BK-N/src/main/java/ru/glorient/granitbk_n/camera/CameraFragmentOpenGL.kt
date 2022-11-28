package ru.glorient.granitbk_n.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
//import kotlinx.android.synthetic.main.video_dialog_camera.*
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.accessory.OpenFileDialog
import ru.glorient.granitbk_n.camera.opengl.CanvasDrawable
import ru.glorient.granitbk_n.camera.opengl.machines.*
import java.io.File
import java.util.*

class CameraFragmentOpenGL : Fragment(R.layout.fragment_camera_test) {
    private lateinit var recordBtn: TextView
    private lateinit var recordBtn2: TextView
    private lateinit var imgRecFront: ImageView
    private lateinit var imgRecBack: ImageView
    private lateinit var buttonPlayFront: ImageButton
    private lateinit var buttonPlayBack: ImageButton
    private lateinit var etTimeSizeFront: EditText
    private lateinit var etTimeSizeBack: EditText

    private lateinit var glSurfaceView: GLSurfaceView
    private val glSurfaceMachine = GLSurfaceMachine(1)
    private val cameraMachine = CameraMachine(1)
    private val encoderMachine = EncoderMachine(1)

    private lateinit var glSurfaceView2: GLSurfaceView
    private val glSurfaceMachine2 = GLSurfaceMachine(0)
    private val cameraMachine2 = CameraMachine(0)
    private val encoderMachine2 = EncoderMachine(0)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        glSurfaceView = view.findViewById(R.id.gl_view)
        glSurfaceView2 = view.findViewById(R.id.gl_view2)

        recordBtn = view.findViewById(R.id.buttonRecFront)
        recordBtn2 = view.findViewById(R.id.buttonRecBack)

        imgRecFront = view.findViewById(R.id.imgRecFront)
        imgRecBack = view.findViewById(R.id.imgRecBack)
        buttonPlayFront = view.findViewById(R.id.buttonPlayFront)
        buttonPlayBack = view.findViewById(R.id.buttonPlayBack)
        etTimeSizeFront = view.findViewById(R.id.etTimeSize)
        etTimeSizeBack = view.findViewById(R.id.etTimeSizeBack)

        glSurfaceMachine.send(
            GLSurfaceAction.Create(
                glSurfaceView,
                CanvasDrawable(),
                encoderMachine.drawingCaller
            )
        )

        glSurfaceMachine2.send(
            GLSurfaceAction.Create(
                glSurfaceView2,
                CanvasDrawable(),
                encoderMachine2.drawingCaller
            )
        )

        encoderMachine.toggleRecordCallback = { isActive ->
            recordBtn.text = getString(
                if (isActive) {
                    imgRecFront.visibility = View.VISIBLE
                    R.string.record_stop
                } else {
                    imgRecFront.visibility = View.INVISIBLE
                    R.string.record_start
                }
            )
        }

        recordBtn.setOnClickListener {
            if (encoderMachine.isActive()) {
                encoderMachine.send(EncoderAction.Stop)
            } else {
                val maxTimeRecording = (etTimeSizeFront.text.toString().toInt()) * 60 * 1000

                if (maxTimeRecording > 0) {
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post(object : Runnable {
                        override fun run() {
                            encoderMachine.send(EncoderAction.Stop)

                            Handler(Looper.getMainLooper()).postAtTime({
                                encoderMachine.send(
                                    EncoderAction.Start(
                                        glSurfaceMachine.encoderProducer
                                    )
                                )
                            }, SystemClock.uptimeMillis() + 500L)

                            Log.d("TAG", "Таймер Front")
                            mainHandler.postDelayed(this, maxTimeRecording.toLong())
                        }
                    })
                }
            }
        }

        encoderMachine2.toggleRecordCallback = { isActive ->
            recordBtn2.text = getString(
                if (isActive) {
                    imgRecBack.visibility = View.VISIBLE
                    R.string.record_stop_back
                } else {
                    imgRecBack.visibility = View.INVISIBLE
                    R.string.record_start_back
                }
            )
        }

        recordBtn2.setOnClickListener {
            if (encoderMachine2.isActive()) {
                encoderMachine2.send(EncoderAction.Stop)
            } else {
                val maxTimeRecording = (etTimeSizeBack.text.toString().toInt()) * 60 * 1000

                if (maxTimeRecording > 0) {
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post(object : Runnable {
                        override fun run() {
                            encoderMachine2.send(EncoderAction.Stop)

                            Handler(Looper.getMainLooper()).postAtTime({
                                encoderMachine2.send(
                                    EncoderAction.Start(
                                        glSurfaceMachine2.encoderProducer
                                    )
                                )
                            }, SystemClock.uptimeMillis() + 500L)

                            Log.d("TAG", "Таймер Back")
                            mainHandler.postDelayed(this, maxTimeRecording.toLong())
                        }
                    })
                }
            }
        }

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
                onClickPlayVideoDialog(fileName)
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
                onClickPlayVideoDialog(fileName)
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

    // Воспроизводим видео
    private fun onClickPlayVideoDialog(path: String) {
        val alertDialog = requireActivity().let {
            val myBuilder = androidx.appcompat.app.AlertDialog.Builder(it)

            val view = requireActivity()
                .layoutInflater
                .inflate(R.layout.video_dialog_camera, null)

            val videoView = view.findViewById<VideoView>(R.id.videoView)
            val mediaController = MediaController(requireContext())
            mediaController.setMediaPlayer(videoView)
            videoView.setOnPreparedListener { mp -> mp.isLooping = true }
            val uri = Uri.fromFile(File(path))
            videoView.setVideoURI(uri)
            videoView.setMediaController(mediaController)
            videoView.requestFocus()
            videoView.start()
            mediaController.show()

            videoView.setOnClickListener {
                videoView.requestFocus()
            }

            myBuilder
                .setView(view)
                .create()
        }

        alertDialog.show()
        alertDialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceMachine.send(GLSurfaceAction.Start)
        cameraMachine.send(
            CameraAction.Start(
                requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager,
                glSurfaceMachine.fullscreenProducer
            )
        )

        glSurfaceMachine2.send(GLSurfaceAction.Start)
        cameraMachine2.send(
            CameraAction.Start(
                requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager,
                glSurfaceMachine2.fullscreenProducer
            )
        )
    }

    override fun onPause() {
        super.onPause()
        glSurfaceMachine.send(GLSurfaceAction.Stop)
        encoderMachine.send(EncoderAction.Stop)
        cameraMachine.send(CameraAction.Stop)

        glSurfaceMachine2.send(GLSurfaceAction.Stop)
        encoderMachine2.send(EncoderAction.Stop)
        cameraMachine2.send(CameraAction.Stop)
    }
}