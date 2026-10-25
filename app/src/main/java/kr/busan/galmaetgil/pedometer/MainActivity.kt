package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isSensorPresent = false

    private lateinit var tvStepCount: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var btnHistory: Button
    private lateinit var expandableListView: ExpandableListView
    private lateinit var gpsTracker: GalmaetgilGpsTracker

    private val mainCourses = listOf("1코스 (임랑~오륙도)", "2코스 (오륙도~부산역)", "3코스 (부산역~태종대)", "4코스 (남항대교~다대포)", "5코스 (을숙도~가덕도)", "6코스 (낙동강~태백사)", "7코스 (구포역~온천천)", "8코스 (온천천~장안사)", "9코스 (회동수원지)")
    private val subCourses = mapOf(
        "1코스 (임랑~오륙도)" to listOf("1-1코스: 일광해수욕장 방면 [진행: 🟩⬜⬜⬜]", "1-2코스: 오륙도 방면 [진행: ⬜⬜⬜⬜]"),
        "2코스 (오륙도~부산역)" to listOf("2-1코스: 광안리 방면 [진행: ⬜⬜⬜⬜]", "2-2코스: 부산역 방면 [진행: ⬜⬜⬜⬜]"),
        "3코스 (부산역~태종대)" to listOf("3-1코스: 영도 절영해안 [진행: ⬜⬜⬜⬜]", "3-2코스: 태종대 방면 [진행: ⬜⬜⬜⬜]"),
        "4코스 (남항대교~다대포)" to listOf("4-1코스: 암남공원 방면 [진행: ⬜⬜⬜⬜]", "4-2코스: 다대포 방면 [진행: ⬜⬜⬜⬜]"),
        "5코스 (을숙도~가덕도)" to listOf("5-1코스: 녹산수문 방면 [진행: ⬜⬜⬜⬜]", "5-2코스: 가덕도 방면 [진행: ⬜⬜⬜⬜]"),
        "6코스 (낙동강~태백사)" to listOf("6-1코스: 삼락공원 방면", "6-2코스: 화명공원 방면", "6-3코스: 태백사 방면"),
        "7코스 (구포역~온천천)" to listOf("7-1코스: 금정산성 방면", "7-2코스: 범어사 방면", "7-3코스: 온천천 방면"),
        "8코스 (온천천~장안사)" to listOf("8-1코스: 회동수원지 입구", "8-2코스: 정관읍 방면", "8-3코스: 장안사 방면"),
        "9코스 (회동수원지)" to listOf("9-1코스: 선동 방면", "9-2코스: 오륜대 방면", "9-3코스: 철마 방면", "9-4코스: 순환 구간")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStepCount = findViewById(R.id.tvStepCount)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        btnHistory = findViewById(R.id.btnHistory)
        expandableListView = findViewById(R.id.expandableListView)

        checkPermissions()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            isSensorPresent = true
        }

        gpsTracker = GalmaetgilGpsTracker(this)
        updateGpsLocation()

        expandableListView.setAdapter(CourseExpandableAdapter(this, mainCourses, subCourses))

        btnHistory.setOnClickListener {
            showMonthlyHistory()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun updateGpsLocation() {
        if (gpsTracker.canGetLocation()) {
            tvGpsStatus.text = "현재 GPS: 위도 ${gpsTracker.latitude}, 경도 ${gpsTracker.longitude}"
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSensorPresent) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        if (isSensorPresent) sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val absoluteSteps = event.values[0].toInt()
            val prefs = getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var baselineSteps = prefs.getInt("BASE_$currentDate", -1)

            if (baselineSteps == -1) {
                baselineSteps = absoluteSteps
                prefs.edit().putInt("BASE_$currentDate", baselineSteps).apply()
            }

            var todaySteps = absoluteSteps - baselineSteps
            if (todaySteps < 0) {
                baselineSteps = absoluteSteps
                prefs.edit().putInt("BASE_$currentDate", baselineSteps).apply()
                todaySteps = 0
            }

            // 오늘 걸음수 화면 및 기록 저장
            tvStepCount.text = todaySteps.toString()
            prefs.edit().putInt("RECORD_$currentDate", todaySteps).apply()
        }
    }

    private fun showMonthlyHistory() {
        val prefs = getSharedPreferences("StepPrefs", Context.MODE_PRIVATE)
        val historyMap = prefs.all.filterKeys { it.startsWith("RECORD_") }
        
        val historyText = if (historyMap.isEmpty()) {
            "아직 저장된 걷기 기록이 없습니다."
        } else {
            historyMap.entries.sortedByDescending { it.key }
                .joinToString("\n") { "${it.key.replace("RECORD_", "")} : ${it.value}보" }
        }

        AlertDialog.Builder(this)
            .setTitle("📅 최근 걷기 기록")
            .setMessage(historyText)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class CourseExpandableAdapter(
    private val context: Context,
    private val mainList: List<String>,
    private val subList: Map<String, List<String>>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = mainList.size
    override fun getChildrenCount(groupPosition: Int): Int = subList[mainList[groupPosition]]?.size ?: 0
    override fun getGroup(groupPosition: Int): Any = mainList[groupPosition]
    override fun getChild(groupPosition: Int, childPosition: Int): Any = subList[mainList[groupPosition]]!![childPosition]
    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()
    override fun hasStableIds(): Boolean = false
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val title = getGroup(groupPosition) as String
        val textView = TextView(context).apply {
            text = "▶ $title"
            textSize = 20f
            setPadding(60, 40, 40, 40)
            setTextColor(android.graphics.Color.parseColor("#111111"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        return textView
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val detail = getChild(groupPosition, childPosition) as String
        val textView = TextView(context).apply {
            text = detail
            textSize = 16f
            setPadding(100, 30, 40, 30)
            setTextColor(android.graphics.Color.parseColor("#444444"))
        }
        return textView
    }
}
