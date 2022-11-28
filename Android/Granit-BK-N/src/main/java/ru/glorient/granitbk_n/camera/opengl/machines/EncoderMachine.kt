package ru.glorient.granitbk_n.camera.opengl.machines

import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.widget.Toast
import ru.glorient.granitbk_n.camera.opengl.grafika.EglCore
import ru.glorient.granitbk_n.camera.opengl.helpers.ContextHelper
import ru.glorient.granitbk_n.camera.opengl.helpers.EncoderHelper
import ru.glorient.granitbk_n.camera.opengl.helpers.OpenGLScene
import ru.glorient.granitbk_n.camera.opengl.helpers.ThreadUtils
import ru.glorient.granitbk_n.camera.opengl.machines.EncoderAction.*
import ru.glorient.granitbk_n.camera.opengl.machines.EncoderState.*
import ru.glorient.granitbk_n.camera.opengl.utils.SimpleProducer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EncoderMachine(val id: Int) :
    StateMachine<EncoderState, EncoderAction> {
    @Volatile
    override var state: EncoderState = WaitingStartRecord()

    private val handlerThread = HandlerThread("encoder handler thread")
    private val handler: Handler
    private val filePath = getFilePath()
//        "${Environment.getExternalStorageDirectory().path}/mediacoder-record.mp4"
//    private val filePath2 =
//        "${Environment.getExternalStorageDirectory().path}/mediacoder-record2.mp4"

    val drawingCaller = object : EncoderDrawingCaller {
        override fun invoke() {
            if (state is RecordAvailable) send(AddFrame)
        }
    }

    var toggleRecordCallback: ToggleRecordCallback? = null

    init {
        handlerThread.start()
        handler = object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                transition(msg.obj as EncoderAction, id)
            }
        }
    }

    private fun getFilePath(): String {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return ""
        // Проверяем есть ли SD карта
        val listOfInternalAndExternalStorage = ContextHelper.appContext.getExternalFilesDirs(null)
        val filePathFolder = if (listOfInternalAndExternalStorage.size >= 2) {
            if (id == 1) {
                ContextHelper.appContext.getExternalFilesDirs("/Front")?.get(1)?.path.orEmpty()
            } else {
                ContextHelper.appContext.getExternalFilesDirs("/Back")?.get(1)?.path.orEmpty()
            }
        } else {
            if (id == 1) {
                ContextHelper.appContext.getExternalFilesDir("/Front")?.path.orEmpty()
            } else {
                ContextHelper.appContext.getExternalFilesDir("/Back")?.path.orEmpty()
            }
        }
        // Указываем путь к папке и называем файл
        val folder = File(filePathFolder)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HH.mm.ss").format(Date())
        return if (id == 1)
            File(folder, "camFront$timeStamp.mp4").path
        else
            File(folder, "camBack$timeStamp.mp4").path
    }

    override fun send(action: EncoderAction) {
        handler.sendMessage(Message.obtain(handler, 0, action))
    }

    override fun transition(action: EncoderAction, id: Int) {
        val state = this.state

        when {
            state is WaitingStartRecord && action is Start -> {
                val mh = state.encoderHolder.copy(surfaceProducer = action.surfaceProducer)
                this.state = WaitingGLContext(mh)
                fetchContext(action)
            }
            state is WaitingGLContext && action is GLContextReady -> {
                val mh = prepareEncoder(state, action, id)
                this.state = RecordAvailable(mh)
            }
            state is RecordAvailable && action is AddFrame -> {
                addFrame(state)
            }
            state !is WaitingStartRecord && action is Stop -> {
                stopRecord(state)
                this.state = WaitingStartRecord()
            }
        }

        notifyChangeActiveState(state)
    }

    fun isActive() = state !is WaitingStartRecord

    private fun fetchContext(action: Start) {
        action.surfaceProducer.consume { triplet ->
            val (context, smallTextureId, fullscreenTextureId) = triplet
            send(
                GLContextReady(
                    context,
                    smallTextureId,
                    fullscreenTextureId
                )
            )
        }
    }

    private fun prepareEncoder(
        state: WaitingGLContext,
        action: GLContextReady,
        id: Int
    ): EncoderHolder {
        val sharedContext = action.sharedEglContext
        val drawableTextureId = action.drawableTextureId
        val cameraTextureId = action.cameraTextureId

        val outputFile = File(getFilePath())
//        val outputFile = File(filePath)

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val encoder = EncoderHelper(
            outputFile = outputFile,
            encoderWidth = 640,
            encoderHeight = 480
//            encoderWidth = 720,
//            encoderHeight = (720f * OpenGLScene.ASPECT_RATIO).toInt()
        )

        val eglCore = EglCore(sharedContext, EglCore.FLAG_RECORDABLE)
        val eglSurface = eglCore.createWindowSurface(encoder.surface)
        eglCore.makeCurrent(eglSurface)

        val openGLScene = OpenGLScene(
            encoder.encoderWidth,
            encoder.encoderHeight,
            fullscreenTextureId = cameraTextureId,
            smallTextureId = drawableTextureId
        )

        return state.encoderHolder.copy(
            eglCore = eglCore,
            eglSurface = eglSurface,
            openGLScene = openGLScene,
            encoder = encoder,
            drawableTextureId = drawableTextureId,
            cameraTextureId = cameraTextureId
        )
    }

    private fun addFrame(state: RecordAvailable) {
        val encoder = state.encoderHolder.encoder ?: return
        val openGLScene = state.encoderHolder.openGLScene ?: return
        val eglCore = state.encoderHolder.eglCore ?: return
        val eglSurface = state.encoderHolder.eglSurface ?: return

        encoder.drainEncoder(false)
        openGLScene.updateFrame()
        eglCore.swapBuffers(eglSurface)
    }

    private fun stopRecord(state: EncoderState) {
        state.encoderHolder.encoder?.release()
        state.encoderHolder.openGLScene?.release()

        showStopToast()
    }

    private fun notifyChangeActiveState(oldState: State) {
        when {
            oldState is WaitingStartRecord && this.state !is WaitingStartRecord -> {
                ThreadUtils.runOnUI {
                    toggleRecordCallback?.invoke(true)
                }
            }

            oldState !is WaitingStartRecord && this.state is WaitingStartRecord -> {
                ThreadUtils.runOnUI {
                    toggleRecordCallback?.invoke(false)
                }
            }
        }
    }

    private fun showStopToast() {
        if (File(filePath).exists()) {
            Toast.makeText(
                ContextHelper.appContext,
                "Stop record: ${filePath}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

sealed class EncoderAction : Action {
    class Start(val surfaceProducer: SimpleProducer<EncoderSurfaceParams>) : EncoderAction()
    class GLContextReady(
        val sharedEglContext: EGLContext,
        val drawableTextureId: Int,
        val cameraTextureId: Int
    ) : EncoderAction()

    object AddFrame : EncoderAction()
    object Stop : EncoderAction()
}

sealed class EncoderState(val encoderHolder: EncoderHolder) : State {
    class WaitingStartRecord() : EncoderState(EncoderHolder())
    class WaitingGLContext(mh: EncoderHolder) : EncoderState(mh)
    class RecordAvailable(mh: EncoderHolder) : EncoderState(mh)
}

data class EncoderHolder(
    val surfaceProducer: SimpleProducer<EncoderSurfaceParams>? = null,
    val eglCore: EglCore? = null,
    val openGLScene: OpenGLScene? = null,
    val encoder: EncoderHelper? = null,
    val eglSurface: EGLSurface? = null,
    val drawableTextureId: Int = 0,
    val cameraTextureId: Int = 0
)

typealias ToggleRecordCallback = (isActive: Boolean) -> Unit

typealias EncoderDrawingCaller = () -> Unit

typealias EncoderSurfaceParams = Triple<EGLContext, Int, Int>