package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    
    private lateinit var tvStepCount: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBarSteps: ProgressBar
    private lateinit var tvDistance: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var btnMap: Button
    
    private lateinit var etMemo: EditText
    private lateinit var btnSaveMemo: Button
    private lateinit var layoutMemos: LinearLayout
    
    private var gpsTracker: GalmaetgilGpsTracker? = null
    private var todaySteps = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            tvStepCount = findViewById(R.id.tvStepCount)
            tvProgressPercent = findViewById(R.id.tvProgressPercent)
            progressBarSteps = findViewById(R.id.progressBarSteps)
            tvDistance = findViewById(R.id.tvDistance)
            tvCalories = findViewById(R.id.tvCalories)
            tvGpsStatus = findViewById(R.id.tvGpsStatus)
            btnMap = findViewById(R.id.btnMap)
            etMemo = findViewById(R.id.etMemo)
            btnSaveMemo = findViewById(R.id.btnSaveMemo)
            layoutMemos = findViewById(R.id.layoutMemos)

            checkPermissions()
            initSensors()
            initGpsSafe()

            loadMemos()
            btnSaveMemo.setOnClickListener {
                val text = etMemo.text.toString()
                if (text.isNotEmpty()) {
                    saveMemo(text)
                    etMemo.text.clear()
                }
            }

            btnMap.setOnClickListener {
                try {
                    if (gpsTracker?.canGetLocation() == true) {
                        val lat = gpsTracker!!.latitude
                        val lon = gpsTracker!!.longitude
                        val uri = "geo:$lat,$lon?q=$lat,$lon(현재위치)"
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    } else {
                        Toast.makeText(this, "GPS 위치를 찾는 중입니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "지도 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun initSensors() {
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initGpsSafe() {
        try {
            gpsTracker = GalmaetgilGpsTracker(this)
            if (gpsTracker?.canGetLocation() == true) {
                val lat = gpsTracker!!.latitude
                val lon = gpsTracker!!.longitude
                tvGpsStatus.text = "현재 좌표: ${String.format("%.4f", lat)}, ${String.format("%.4f", lon)}"
            }
        } catch (e: Exception) {
            tvGpsStatus.text = "GPS 초기화 지연 중..."
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                val absoluteSteps = event.values[0].toInt()
                val prefs = getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var baselineSteps = prefs.getInt("BASE_$currentDate", -1)

                if (baselineSteps == -1) {
                    baselineSteps = absoluteSteps
                    prefs.edit().putInt("BASE_$currentDate", baselineSteps).apply()
                }

                todaySteps = absoluteSteps - baselineSteps
                if (todaySteps < 0) {
                    baselineSteps = absoluteSteps
                    prefs.edit().putInt("BASE_$currentDate", baselineSteps).apply()
                    todaySteps = 0
                }
                updateUI()
            }
        } catch (e: Exception) {}
    }

    private fun updateUI() {
        tvStepCount.text = String.format("%,d", todaySteps)
        progressBarSteps.progress = todaySteps
        val percent = (todaySteps / 10000.0) * 100
        tvProgressPercent.text = "${percent.toInt()}% 달성"

        val distanceKm = (todaySteps * 0.7) / 1000.0
        val calories = (todaySteps * 0.04).toInt()

        tvDistance.text = String.format("%.1f km", distanceKm)
        tvCalories.text = "$calories kcal"
    }

    private fun saveMemo(content: String) {
        val prefs = getSharedPreferences("MemoPrefs", Context.MODE_PRIVATE)
        val time = SimpleDateFormat("MM월 dd일 a hh:mm", Locale.KOREA).format(Date())
        val currentMemos = prefs.getString("MEMO_LIST", "") ?: ""
        
        val newMemo = "[$time] $content"
        val updatedMemos = if (currentMemos.isEmpty()) newMemo else "$newMemo||$currentMemos"
        
        prefs.edit().putString("MEMO_LIST", updatedMemos).apply()
        loadMemos()
    }

    private fun loadMemos() {
        try {
            layoutMemos.removeAllViews()
            val prefs = getSharedPreferences("MemoPrefs", Context.MODE_PRIVATE)
            val memos = prefs.getString("MEMO_LIST", "")?.split("||")?.filter { it.isNotEmpty() } ?: listOf()

            for (memo in memos) {
                val textView = TextView(this).apply {
                    text = memo
                    textSize = 14f
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(android.graphics.Color.parseColor("#FFFFF0"))
                }
                layoutMemos.addView(textView)
                
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                    setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                }
                layoutMemos.addView(divider)
            }
        } catch (e: Exception) {}
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
