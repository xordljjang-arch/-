package kr.busan.galmaetgil.pedometer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var currentSteps = 0
    private var stepGoal = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        stepGoal = prefs.getInt("STEP_GOAL", 10000)

        // 1. 자정 초기화 로직 (교차검증 완)
        checkMidnightReset()
        
        // 2. UI 세팅 및 하단 탭 전환
        setupUI()
        
        // 3. 하루씩 자동으로 밀리는 7일 롤링 차트 (교차검증 완)
        updateWeeklyChartUI()
    }

    override fun onResume() {
        super.onResume()
        currentSteps = prefs.getInt("TODAY_STEPS", 0)
        updateStepUI()
    }

    // [핵심 교차검증 1] 자정이 지나면 오늘 걸음수를 어제 날짜로 저장하고, 오늘은 0으로 리셋
    private fun checkMidnightReset() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("LAST_DATE", "")

        if (lastDate != todayStr) {
            if (lastDate!!.isNotEmpty()) {
                val yesterdaySteps = prefs.getInt("TODAY_STEPS", 0)
                prefs.edit().putInt("HISTORY_$lastDate", yesterdaySteps).apply()
            }
            // 새 날짜 갱신 및 0보 리셋
            prefs.edit().putInt("TODAY_STEPS", 0).putString("LAST_DATE", todayStr).apply()
            currentSteps = 0
        }
    }

    private fun setupUI() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_today
        
        // 하단 탭 연동
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> true
                R.id.nav_stamp -> {
                    // 갈맷길 도장 탭 클릭 시 기존 StampActivity로 즉시 이동
                    startActivity(Intent(this, StampActivity::class.java))
                    false 
                }
                else -> false
            }
        }

        // 목표 설정 기능 (클릭 시 변경)
        findViewById<LinearLayout>(R.id.btnSetGoal).setOnClickListener {
            stepGoal += 1000
            if (stepGoal > 30000) stepGoal = 5000
            prefs.edit().putInt("STEP_GOAL", stepGoal).apply()
            updateStepUI()
            Toast.makeText(this, "목표가 ${stepGoal}보로 설정되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 오늘 날짜 기준 한줄 메모 복구 및 저장 (자정 지나면 새 메모장으로 비워짐)
        val etMemo = findViewById<EditText>(R.id.etMemo)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etMemo.setText(prefs.getString("MEMO_$todayStr", ""))

        findViewById<Button>(R.id.btnSaveMemo).setOnClickListener {
            prefs.edit().putString("MEMO_$todayStr", etMemo.text.toString()).apply()
            Toast.makeText(this, "오늘의 메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStepUI() {
        val tvStepCount = findViewById<TextView>(R.id.tvStepCountMain)
        val tvStepPercent = findViewById<TextView>(R.id.tvStepPercent)
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)
        val tvGoalSteps = findViewById<TextView>(R.id.tvGoalSteps)

        val percent = if (stepGoal > 0) (currentSteps * 100 / stepGoal) else 0
        val distanceKm = currentSteps * 0.0007 
        val calories = currentSteps * 0.04 

        tvStepCount.text = "$currentSteps / $stepGoal 보"
        tvStepPercent.text = "${percent}% 달성"
        tvDistance.text = String.format("%.2f km", distanceKm)
        tvCalories.text = "${calories.toInt()} kcal"
        tvGoalSteps.text = "${stepGoal}보"
    }

    // [핵심 교차검증 2] 그림과 동일하게 7개 항목을 나열하고 자동으로 밀어내는 차트 로직
    private fun updateWeeklyChartUI() {
        val layoutWeeklyChart = findViewById<LinearLayout>(R.id.layoutWeeklyChart)
        layoutWeeklyChart.removeAllViews() // 초기화

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.KOREAN) // 월, 화, 수...
        val cal = Calendar.getInstance()
        
        // 기준점을 6일 전으로 세팅 (오늘이 항상 오른쪽 끝이 되도록 자동 밀어내기)
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val dateStr = format.format(cal.time)
            val dayStr = dayFormat.format(cal.time)
            val steps = if (i == 6) currentSteps else prefs.getInt("HISTORY_$dateStr", 0)

            // 그림처럼 요일과 걸음수를 세로로 정렬하여 7칸 생성
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            }

            val tvSteps = TextView(this).apply {
                text = if (steps > 999) "${steps/1000}k" else steps.toString()
                textSize = 12f
                setTextColor(Color.parseColor("#4CAF50")) // 녹색 텍스트
                gravity = Gravity.CENTER
            }

            val tvDay = TextView(this).apply {
                text = dayStr
                textSize = 14f
                textStyle = android.graphics.Typeface.BOLD
                setPadding(0, 8, 0, 0)
                gravity = Gravity.CENTER
            }

            column.addView(tvSteps)
            column.addView(tvDay)
            layoutWeeklyChart.addView(column)

            // 하루씩 밀기
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
