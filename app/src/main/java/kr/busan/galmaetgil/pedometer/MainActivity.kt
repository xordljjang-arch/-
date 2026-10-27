package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var prefs: SharedPreferences
    private var currentSteps = 0
    private var stepGoal = 10000

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 블랙박스 에러 추적
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val errorText = "앱 강제종료 원인:\n\n${e.stackTraceToString()}"
            getSharedPreferences("BlackBox", Context.MODE_PRIVATE)
                .edit().putString("crash_log", errorText).commit()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }

        super.onCreate(savedInstanceState)

        val crashPrefs = getSharedPreferences("BlackBox", Context.MODE_PRIVATE)
        val crashLog = crashPrefs.getString("crash_log", null)
        if (crashLog != null) {
            crashPrefs.edit().remove("crash_log").apply()
            val errorView = TextView(this).apply {
                text = crashLog
                setTextColor(Color.RED)
                setPadding(32, 32, 32, 32)
                textSize = 12f
            }
            val scrollView = ScrollView(this).apply { addView(errorView) }
            setContentView(scrollView)
            return
        }

        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        stepGoal = prefs.getInt("STEP_GOAL", 10000)

        // 1. 센서 교체: 기기 호환성이 가장 높고 정확한 누적 걸음수(STEP_COUNTER) 사용
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "걸음 센서가 없는 기기입니다.", Toast.LENGTH_LONG).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
            }
        }

        checkMidnightReset()
        setupUI()
        updateWeeklyChartUI()
    }

    override fun onResume() {
        super.onResume()
        checkMidnightReset() 
        currentSteps = prefs.getInt("TODAY_STEPS", 0)
        updateStepUI()

        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            checkMidnightReset() // 걷는 중 자정이 지날 경우 대비

            val totalSteps = event.values[0]
            var dayStart = prefs.getFloat("DAY_START_COUNTER", -1f)

            // 최초 측정이거나, 기기 재부팅으로 센서값이 0으로 초기화된 경우 기준점 다시 잡기
            if (dayStart == -1f || totalSteps < dayStart) {
                dayStart = totalSteps - currentSteps
                prefs.edit().putFloat("DAY_START_COUNTER", dayStart).apply()
            }

            currentSteps = (totalSteps - dayStart).toInt()
            prefs.edit().putInt("TODAY_STEPS", currentSteps).apply()

            updateStepUI()
            updateWeeklyChartUI() // 오늘 걷는 현황을 주간 차트에도 즉시 포함
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // 2. 자정 자동 리셋 기능
    private fun checkMidnightReset() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("LAST_DATE", "") ?: ""

        if (lastDate.isNotEmpty() && lastDate != todayStr) {
            val yesterdaySteps = prefs.getInt("TODAY_STEPS", 0)
            prefs.edit().putInt("HISTORY_$lastDate", yesterdaySteps).apply()

            prefs.edit()
                .putInt("TODAY_STEPS", 0)
                .putString("LAST_DATE", todayStr)
                .putFloat("DAY_START_COUNTER", -1f) // 센서 기준점 리셋
                .apply()
            currentSteps = 0
        } else if (lastDate.isEmpty()) {
            prefs.edit().putString("LAST_DATE", todayStr).apply()
        }
    }

    private fun setupUI() {
        findViewById<LinearLayout>(R.id.nav_stamp_btn)?.setOnClickListener {
            startActivity(Intent(this, StampActivity::class.java))
        }

        // 4. 목표 설정 시 숫자 직접 타이핑 입력 기능
        findViewById<LinearLayout>(R.id.btnSetGoal)?.setOnClickListener {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "예: 10000"
                setText(stepGoal.toString())
            }

            AlertDialog.Builder(this)
                .setTitle("목표 걸음수 설정")
                .setView(input)
                .setPositiveButton("저장") { _, _ ->
                    val newGoal = input.text.toString().toIntOrNull()
                    if (newGoal != null && newGoal > 0) {
                        stepGoal = newGoal
                        prefs.edit().putInt("STEP_GOAL", stepGoal).apply()
                        updateStepUI()
                        updateWeeklyChartUI()
                        Toast.makeText(this, "목표가 ${stepGoal}보로 설정되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }

        val etMemo = findViewById<EditText>(R.id.etMemo)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etMemo?.setText(prefs.getString("MEMO_$todayStr", ""))

        findViewById<Button>(R.id.btnSaveMemo)?.setOnClickListener {
            prefs.edit().putString("MEMO_$todayStr", etMemo?.text.toString()).apply()
            Toast.makeText(this, "오늘의 메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStepUI() {
        val percent = if (stepGoal > 0) (currentSteps * 100 / stepGoal) else 0
        val distanceKm = currentSteps * 0.0007
        val calories = currentSteps * 0.04

        findViewById<TextView>(R.id.tvStepCountMain)?.text = "$currentSteps / $stepGoal 보"
        findViewById<TextView>(R.id.tvStepPercent)?.text = "${percent}% 달성"
        findViewById<TextView>(R.id.tvDistance)?.text = String.format("%.2f km", distanceKm)
        findViewById<TextView>(R.id.tvCalories)?.text = "${calories.toInt()} kcal"
        findViewById<TextView>(R.id.tvGoalSteps)?.text = "${stepGoal}보"
    }

    // 3. 이번 주 월~일 고정 배치 및 오늘 명확한 표시
    private fun updateWeeklyChartUI() {
        val layoutWeeklyChart = findViewById<LinearLayout>(R.id.layoutWeeklyChart)
        layoutWeeklyChart?.removeAllViews()
        layoutWeeklyChart?.orientation = LinearLayout.VERTICAL

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("MM/dd (E)", Locale.KOREAN)
        val todayStr = format.format(Date())

        // 기준을 이번 주 월요일로 맞춤
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {
            val targetDateStr = format.format(cal.time)
            val dayStr = dayFormat.format(cal.time)

            val isToday = targetDateStr == todayStr
            val steps = if (isToday) currentSteps else prefs.getInt("HISTORY_$targetDateStr", 0)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, 16, 0, 16)
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvDay = TextView(this).apply {
                text = if (isToday) "$dayStr [오늘]" else dayStr
                textSize = 13f
                setTextColor(if (isToday) Color.parseColor("#E65100") else Color.DKGRAY)
                setTypeface(null, if (isToday) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
            }

            val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
                max = stepGoal
                progress = steps
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    val barColor = if (isToday) "#FF9800" else "#4CAF50"
                    progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(barColor))
                }
            }

            val tvSteps = TextView(this).apply {
                text = "${steps}보"
                textSize = 13f
                setTextColor(if (isToday) Color.parseColor("#E65100") else Color.parseColor("#2E7D32"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                setPadding(16, 0, 0, 0)
            }

            row.addView(tvDay)
            row.addView(progressBar)
            row.addView(tvSteps)
            layoutWeeklyChart?.addView(row)

            cal.add(Calendar.DAY_OF_YEAR, 1) // 다음 요일로 하루씩 이동
        }
    }
}
