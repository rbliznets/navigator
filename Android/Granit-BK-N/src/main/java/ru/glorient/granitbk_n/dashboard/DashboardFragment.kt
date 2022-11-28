package ru.glorient.granitbk_n.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.sccomponents.gauges.gr008.GR008
import ru.glorient.granitbk_n.R
import ru.glorient.granitbk_n.accessory.UpdateSpeedListener

class DashboardFragment: Fragment(R.layout.fragment_dashboard), UpdateSpeedListener {
//    private lateinit var speedometer: GaugeView
//    private lateinit var ranges: GaugeView
//    private lateinit var tachometer: GaugeView
    private lateinit var speedometer: GR008
    private lateinit var ranges: GR008
    private lateinit var tachometer: GR008

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        speedometer = view.findViewById<View>(R.id.speedometer) as GaugeView
//        ranges = view.findViewById<View>(R.id.ranges) as GaugeView
//        tachometer = view.findViewById<View>(R.id.tachometer) as GaugeView
        speedometer = view.findViewById<View>(R.id.speedometer) as GR008
        ranges = view.findViewById<View>(R.id.ranges) as GR008
        tachometer = view.findViewById<View>(R.id.tachometer) as GR008
    }

    override fun updateSpeed(speed: Int) {
//        speedometer.setTargetValue(speed.toDouble())
//        ranges.setTargetValue(0f)
//        tachometer.setTargetValue(0f)

        speedometer.value = speed.toDouble()
        ranges.value = 0.0
        tachometer.value = 0.0
    }
}