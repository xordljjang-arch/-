package kr.busan.galmaetgil.pedometer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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

        checkMidnightReset()
        setupUI()
        updateWeeklyChart()
    }

    override fun onResume() {
        super.onResume()
        // 센서 등에서 걸음수를 올려주는 로직이 있다면 여기서 currentSteps를 갱신합니다.
        // 현재는 저장된 값을 불러옵니다.
        currentSteps = prefs.getInt("TODAY_STEPS", 0)
        updateStepUI()
    }

    private fun checkMidnightReset() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("LAST_DATE", "")

        if (lastDate != todayStr) {
            // 날짜가 변경됨 (자정 지남) -> 어제 걸음수 히스토리에 저장 후 오늘 0으로 초기화
            if (lastDate!!.isNotEmpty()) {
                val yesterdaySteps = prefs.getInt("TODAY_STEPS", 0)
                prefs.edit().putInt("HISTORY_$lastDate", yesterdaySteps).apply()
            }
            prefs.edit().putInt("TODAY_STEPS", 0).putString("LAST_DATE", todayStr).apply()
            currentSteps = 0
        }
    }

    private fun setupUI() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_today
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> true
                R.id.nav_stamp -> {
                    startActivity(Intent(this, StampActivity::class.java))
                    false // 탭 선택 상태는 메인에 머물도록 유지
                }
                else -> false
            }
        }

        findViewById<LinearLayout>(R.id.btnSetGoal).setOnClickListener {
            // 임시로 누를때마다 1000보씩 증가하는 로직 (실제로는 다이얼로그로 구현 권장)
            stepGoal += 1000
            if (stepGoal > 30000) stepGoal = 5000
            prefs.edit().putInt("STEP_GOAL", stepGoal).apply()
            updateStepUI()
            Toast.makeText(this, "목표가 ${stepGoal}보로 변경되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 메모 기능 복구 및 당일 기준 저장
        val etMemo = findViewById<EditText>(R.id.etMemo)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etMemo.setText(prefs.getString("MEMO_$todayStr", ""))

        findViewById<Button>(R.id.btnSaveMemo).setOnClickListener {
            prefs.edit().putString("MEMO_$todayStr", etMemo.text.toString()).apply()
            Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStepUI() {
        val tvStepCount = findViewById<TextView>(R.id.tvStepCountMain)
        val tvStepPercent = findViewById<TextView>(R.id.tvStepPercent)
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)
        val tvGoalSteps = findViewById<TextView>(R.id.tvGoalSteps)

        val percent = if (stepGoal > 0) (currentSteps * 100 / stepGoal) else 0
        val distanceKm = currentSteps * 0.0007 // 보폭 임의 계산
        val calories = currentSteps * 0.04 // 칼로리 임의 계산

        tvStepCount.text = "$currentSteps / $stepGoal 보"
        tvStepPercent.text = "${percent}% 달성"
        tvDistance.text = String.format("%.2f km", distanceKm)
        tvCalories.text = "${calories.toInt()} kcal"
        tvGoalSteps.text = "${stepGoal}보"
    }

    private fun updateWeeklyChart() {
        val tvWeeklyChart = findViewById<TextView>(R.id.tvWeeklyChart)
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.KOREAN)

        var chartText = ""
        // 오늘을 기준으로 과거 6일부터 오늘까지 총 7일 연속 표기 (자동 밀리기)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val dateStr = format.format(cal.time)
            val dayStr = dayFormat.format(cal.time)
            val steps = if (i == 6) currentSteps else prefs.getInt("HISTORY_$dateStr", 0)
            
            chartText += "$dayStr: ${steps}보" + if (i < 6) "\n" else ""
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        tvWeeklyChart.text = chartText
    }
}
