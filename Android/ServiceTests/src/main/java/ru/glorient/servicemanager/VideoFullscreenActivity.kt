package ru.glorient.servicemanager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONException
import org.json.JSONObject
import ru.glorient.bkn.InformerService
import ru.glorient.servicemanager.databinding.ActivityVideoFullscreenBinding
import java.io.File

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VideoFullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoFullscreenBinding
    private lateinit var fullscreenContent: VideoView
    private val hideHandler = Handler()

    // declaring a null variable for MediaController
    private var mediaControls: MediaController? = null

    var displayManager: DisplayManager? = null
    private class DisplayListiner(val mParent: VideoFullscreenActivity): DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
        }

        override fun onDisplayChanged(displayId: Int) {
        }

        override fun onDisplayRemoved(displayId: Int) {
            if((mParent.displayManager?.displays?.size ?: 0) < 2){
                if(mParent.vid != 0L){
                    val intent = Intent(InformerService.toInName)
                    intent.putExtra("payload", JSONObject("""{"endvideo":${mParent.vid}}""").toString())
                    LocalBroadcastManager.getInstance(mParent).sendBroadcast(intent)
                }
                mParent.finish()
            }
        }
    }
    private val mDisplayListiner = DisplayListiner(this)

    var vid: Long = 0L
    private val mInformerBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val str = intent.getStringExtra("payload")
            if (str != null) {
                try {
                    val msg = JSONObject(str)
                    if (msg.has("video")) {
                        val video = msg.getJSONObject("video")
                        fullscreenContent.stopPlayback()
                        if(vid != 0L){
                            val intent1 = Intent(InformerService.toInName)
                            intent1.putExtra("payload", JSONObject("""{"endvideo":${vid}}""").toString())
                            LocalBroadcastManager.getInstance(this@VideoFullscreenActivity).sendBroadcast(intent1)
                            vid = 0
                        }
                        vid=video.getLong("id")
                        if(vid != 0L){
                            fullscreenContent.background = null
                            fullscreenContent.setVideoURI(Uri.fromFile(File(video.getString("file"))))
                            fullscreenContent.start()
                        }
                    }
                } catch (e: JSONException) {
                    Log.w("VideoActivity", e.toString())
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (displayManager == null) {
            displayManager = this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager?.registerDisplayListener( mDisplayListiner,null)
        }

        binding = ActivityVideoFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent
        //fullscreenContent.setOnClickListener { toggle() }
        if (mediaControls == null) {
            // creating an object of media controller class
            mediaControls = MediaController(this)

            // set the anchor view for the video view
            mediaControls!!.setAnchorView(fullscreenContent)
        }

        fullscreenContent.setOnCompletionListener {
            if(vid != 0L){
                val intent = Intent(InformerService.toInName)
                intent.putExtra("payload", JSONObject("""{"endvideo":${vid}}""").toString())
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                vid=0
            }
        }

        fullscreenContent.setOnErrorListener { mp, what, extra ->
            Toast.makeText(applicationContext, "An Error Occured " +
                    "While Playing Video !!!", Toast.LENGTH_LONG).show()
            false
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mInformerBroadcastReceiver,
            IntentFilter(InformerService.toOutName)
        )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}