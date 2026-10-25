package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isSensorPresent = false
    private var totalSteps = 0

    private lateinit var tvStepCount: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var listViewCourses: ListView

    private lateinit var gpsTracker: GalmaetgilGpsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 간단하고 직관적인 레이아웃을 코드로 직접 구성 (어르신용 큰 글씨)
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F6F8"))
        }

        // 1. 타이틀
        val tvTitle = TextView(this).apply {
            text = "🚶 부산 갈맷길 시니어 만보기"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#111111"))
            setPadding(0, 0, 0, 20)
        }
        linearLayout.addView(tvTitle)

        // 2. 만보기 표시 박스
        tvStepCount = TextView(this).apply {
            text = "오늘의 걸음수: 측정 중..."
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#0066CC"))
            setPadding(0, 10, 0, 10)
        }
        linearLayout.addView(tvStepCount)

        // 3. GPS 상태 표시 박스
        tvGpsStatus = TextView(this).apply {
            text = "GPS 위치: 확인 중..."
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            setPadding(0, 10, 0, 20)
        }
        linearLayout.addView(tvGpsStatus)

        // 4. 코스 목록 안내 라벨
        val tvCourseLabel = TextView(this).apply {
            text = "🗺️ 부산 갈맷길 전체 코스 안내"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#222222"))
            setPadding(0, 20, 0, 10)
        }
        linearLayout.addView(tvCourseLabel)

        // 5. 전체 갈맷길 코스 리스트뷰
        listViewCourses = ListView(this)
        linearLayout.addView(listViewCourses)

        setContentView(linearLayout)

        // 권한 체크 및 요청
        checkPermissions()

        // 만보기 센서 초기화
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            isSensorPresent = true
        } else {
            tvStepCount.text = "기기에 만보기 센서가 없습니다."
        }

        // GPS 트래커 초기화
        gpsTracker = GalmaetgilGpsTracker(this)
        updateGpsLocation()

        // 전체 코스 데이터 연결 (아까 만든 CourseRepository의 데이터 활용)
        val courseNames = galmaetgilSubCourses.map { "${it.name} (${it.distance})\n- ${it.description}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, courseNames)
        listViewCourses.adapter = adapter

        listViewCourses.setOnItemClickListener { _, _, position, _ ->
            val selected = galmaetgilSubCourses[position]
            Toast.MakedText(this, "선택된 코스: ${selected.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun updateGpsLocation() {
        if (gpsTracker.canGetLocation()) {
            val latitude = gpsTracker.latitude
            val longitude = gpsTracker.longitude
            tvGpsStatus.text = "GPS 위치: 위도 $latitude, 경도 $longitude"
        } else {
            tvGpsStatus.text = "GPS 신호를 잡지 못했습니다."
        }
    }

    override fun onResume() {
        super.onResume()
        if (isSensorPresent && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSensorPresent) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = event.values[0].toInt()
            tvStepCount.text = "오늘의 걸음수: ${totalSteps}보"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
