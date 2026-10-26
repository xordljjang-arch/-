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
        
        // ★ 에러 추적을 위한 강력한 예외 처리 (앱 강제종료 방지)
        try {
            setContentView(R.layout.activity_main)

            prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
            stepGoal = prefs.getInt("STEP_GOAL", 10000)

            checkMidnightReset()
            setupUI()
            updateWeeklyChartUI()
        } catch (e: Exception) {
            // 튕기는 대신 화면에 에러 원인을 텍스트로 출력합니다!
            val errorView = TextView(this).apply {
                text = "앱 실행 중 오류 발생 (화면을 캡쳐해서 알려주세요!):\n\n${e.stackTraceToString()}"
                setTextColor(Color.RED)
                setPadding(32, 32, 32, 32)
                textSize = 12f
            }
            setContentView(errorView)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            currentSteps = prefs.getInt("TODAY_STEPS", 0)
            updateStepUI()
        } catch (e: Exception) {
            // 무시
        }
    }

    private fun checkMidnightReset() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Null 예외 발생 가능성이 있던 부분을 가장 안전하게 수정했습니다.
        val lastDate = prefs.getString("LAST_DATE", "") ?: "" 

        if (lastDate != todayStr) {
            if (lastDate.isNotEmpty()) {
                val yesterdaySteps = prefs.getInt("TODAY_STEPS", 0)
                prefs.edit().putInt("HISTORY_$lastDate", yesterdaySteps).apply()
            }
            prefs.edit().putInt("TODAY_STEPS", 0).putString("LAST_DATE", todayStr).apply()
            currentSteps = 0
        }
    }

    private fun setupUI() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_today
        
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> true
                R.id.nav_stamp -> {
                    startActivity(Intent(this, StampActivity::class.java))
                    false 
                }
                else -> false
            }
        }

        findViewById<LinearLayout>(R.id.btnSetGoal)?.setOnClickListener {
            stepGoal += 1000
            if (stepGoal > 30000) stepGoal = 5000
            prefs.edit().putInt("STEP_GOAL", stepGoal).apply()
            updateStepUI()
            Toast.makeText(this, "목표가 ${stepGoal}보로 설정되었습니다.", Toast.LENGTH_SHORT).show()
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
        val tvStepCount = findViewById<TextView>(R.id.tvStepCountMain)
        val tvStepPercent = findViewById<TextView>(R.id.tvStepPercent)
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val tvCalories = findViewById<TextView>(R.id.tvCalories)
        val tvGoalSteps = findViewById<TextView>(R.id.tvGoalSteps)

        val percent = if (stepGoal > 0) (currentSteps * 100 / stepGoal) else 0
        val distanceKm = currentSteps * 0.0007 
        val calories = currentSteps * 0.04 

        tvStepCount?.text = "$currentSteps / $stepGoal 보"
        tvStepPercent?.text = "${percent}% 달성"
        tvDistance?.text = String.format("%.2f km", distanceKm)
        tvCalories?.text = "${calories.toInt()} kcal"
        tvGoalSteps?.text = "${stepGoal}보"
    }

    private fun updateWeeklyChartUI() {
        val layoutWeeklyChart = findViewById<LinearLayout>(R.id.layoutWeeklyChart)
        layoutWeeklyChart?.removeAllViews()

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.KOREAN) 
        val cal = Calendar.getInstance()
        
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val dateStr = format.format(cal.time)
            val dayStr = dayFormat.format(cal.time)
            val steps = if (i == 6) currentSteps else prefs.getInt("HISTORY_$dateStr", 0)

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            }

            val tvSteps = TextView(this).apply {
                text = if (steps > 999) "${steps/1000}k" else steps.toString()
                textSize = 12f
                setTextColor(Color.parseColor("#4CAF50"))
                gravity = Gravity.CENTER
            }

            val tvDay = TextView(this).apply {
                text = dayStr
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 0)
                gravity = Gravity.CENTER
            }

            column.addView(tvSteps)
            column.addView(tvDay)
            layoutWeeklyChart?.addView(column)

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
