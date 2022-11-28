package ru.glorient.granitbk_n.camera.opengl.helpers

import android.opengl.EGL14
import android.opengl.GLES20
import android.view.Surface
import ru.glorient.granitbk_n.camera.opengl.utils.ErrorUtils.checkGlError
import ru.glorient.granitbk_n.camera.opengl.utils.ShaderUtils

class OpenGLScene(
    sceneWidth: Int,
    sceneHeight: Int,
    fullscreenTextureId: Int? = null,
    smallTextureId: Int? = null
) {
    private val programId: Int
    private val uMVPMatrixHandle: Int
    private val uTexMatrixHandle: Int
    private val aPositionHandle: Int
    private val aTextureCoordHandler: Int

    private val vertexShader = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uTexMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;

        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTextureCoord = (uTexMatrix * aTextureCoord).xy;
        }
    """.trimIndent()

    private val fragmentShader = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;

        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoord);
        }
    """.trimIndent()

    val openglContext = EGL14.eglGetCurrentContext()

    val fullscreenTexture = OpenGLExternalTexture(
        textureWidth = sceneWidth,
        textureHeight = sceneHeight,
        verticesData = floatArrayOf(
           // X,   Y,      Z,     U,  V
            -1.0f,  -1.0f,  0.0f,  1f, 0f,
             1.0f,  -1.0f,  0.0f,  0f, 0f,
            -1.0f,   1.0f,  0.0f,  1f, 1f,
             1.0f,   1.0f,  0.0f,  0f, 1f
        ),
        externalTextureId = fullscreenTextureId,
        rotate = Surface.ROTATION_180
    )

    val smallTexture = OpenGLExternalTexture(
        textureWidth = (0.75f * sceneWidth).toInt(),
//        textureWidth = sceneWidth,
        textureHeight = (0.05f * sceneHeight).toInt(),
//        textureHeight = sceneHeight,
        verticesData = floatArrayOf(
           // X,   Y,      Z,     U,  V
             -1.0f,   0.85f,  0.0f,  0f, 0f,
             1.0f,   0.85f,  0.0f,  0f, 1f,
             -1.0f,   1.0f,  0.0f,  1f, 0f,
             1.0f,   1.0f,  0.0f,  1f, 1f
//           // X,   Y,      Z,     U,  V
//             -1.0f,   0.95f,  0.0f,  0f, 0f,
//             1.0f,   0.95f,  0.0f,  0f, 1f,
//             -1.0f,   1.0f,  0.0f,  1f, 0f,
//             1.0f,   1.0f,  0.0f,  1f, 1f
        ),
        externalTextureId = smallTextureId,
        rotate = Surface.ROTATION_270
    )

    init {
        GLES20.glViewport(0, 0, sceneWidth, sceneHeight)

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader)
        if (programId == 0) {
            throw java.lang.RuntimeException("Could not create shader program")
        }

        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (aPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (uMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uMVPMatrix")
        }

        uTexMatrixHandle = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        checkGlError("glGetUniformLocation uTexMatrix")
        if (uTexMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uTexMatrix")
        }

        aTextureCoordHandler = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (aTextureCoordHandler == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }

    }

    fun release() {
        smallTexture.release()
        fullscreenTexture.release()
    }

    fun updateFrame() {
        GLES20.glClearColor(1.0f, 1f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programId)
        checkGlError("glUseProgram")

        fullscreenTexture.updateFrame(
            aPositionHandle = aPositionHandle,
            aTextureCoordHandler = aTextureCoordHandler,
            uTexHandler = uTexMatrixHandle,
            uMvpHandler = uMVPMatrixHandle
        )

        smallTexture.updateFrame(
            aPositionHandle = aPositionHandle,
            aTextureCoordHandler = aTextureCoordHandler,
            uTexHandler = uTexMatrixHandle,
            uMvpHandler = uMVPMatrixHandle
        )
    }

    companion object {
        const val ASPECT_RATIO = 16f / 9f
    }
}