package kr.busan.galmaetgil.pedometer

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var dailyGoal = 10000
    private lateinit var tvDailyGoal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDailyGoal = findViewById(R.id.tvDailyGoal)
        
        // 저장된 목표 걸음 수 불러오기
        val prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        dailyGoal = prefs.getInt("DAILY_GOAL", 10000)
        updateGoalText()

        findViewById<ImageButton>(R.id.btnSetGoal).setOnClickListener {
            showGoalSettingDialog()
        }

        drawWeeklyChart()
    }

    private fun updateGoalText() {
        tvDailyGoal.text = "일일 목표: %,d보".format(dailyGoal)
    }

    private fun showGoalSettingDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(dailyGoal.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("목표 걸음 수 설정")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                val newGoal = input.text.toString().toIntOrNull()
                if (newGoal != null && newGoal > 0) {
                    dailyGoal = newGoal
                    getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
                        .edit().putInt("DAILY_GOAL", dailyGoal).apply()
                    updateGoalText()
                    drawWeeklyChart() // 목표가 바뀌면 왕관 달성 여부도 재계산
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun drawWeeklyChart() {
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        chartContainer.removeAllViews()

        // 테스트용 주간 걸음 수 데이터 (월~일)
        val days = listOf("월", "화", "수", "목", "금", "토", "일")
        val weeklySteps = listOf(8500, 11000, 7000, 10500, 9000, 15000, 4000)
        
        val maxSteps = weeklySteps.maxOrNull()?.coerceAtLeast(dailyGoal) ?: dailyGoal

        for (i in days.indices) {
            val steps = weeklySteps[i]
            val isGoalReached = steps >= dailyGoal

            val dayLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            // 막대 그래프
            val barHeightWeight = (steps.toFloat() / maxSteps.toFloat()) * 0.7f // 최대 높이의 70%까지 차지
            
            // 막대 위 여백
            val spacer = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f - barHeightWeight)
            }
            
            val bar = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 0, barHeightWeight)
                setBackgroundColor(if (isGoalReached) Color.parseColor("#4CAF50") else Color.parseColor("#90CAF9"))
            }

            // 요일 텍스트
            val tvDay = TextView(this).apply {
                text = days[i]
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 4)
            }

            // 달성 왕관 (★ 이모지로 대체 표현)
            val tvCrown = TextView(this).apply {
                text = "👑"
                textSize = 12f
                gravity = Gravity.CENTER
                alpha = if (isGoalReached) 1f else 0f // 목표 달성 시에만 표시
            }

            dayLayout.addView(spacer)
            dayLayout.addView(bar)
            dayLayout.addView(tvDay)
            dayLayout.addView(tvCrown)

            chartContainer.addView(dayLayout)
        }
    }
}
