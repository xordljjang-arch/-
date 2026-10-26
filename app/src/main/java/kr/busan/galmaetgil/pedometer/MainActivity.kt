package kr.busan.galmaetgil.pedometer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var dailyGoal = 10000
    private lateinit var tvDailyGoal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDailyGoal = findViewById(R.id.tvDailyGoal)
        val prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        dailyGoal = prefs.getInt("DAILY_GOAL", 10000)
        updateGoalText()

        findViewById<ImageButton>(R.id.btnSetGoal).setOnClickListener {
            showGoalSettingDialog()
        }

        // 갈맷길 지도 화면으로 넘어가는 핵심 버튼
        findViewById<Button>(R.id.btnOpenGalmaetgil).setOnClickListener {
            startActivity(Intent(this, StampActivity::class.java))
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
                    drawWeeklyChart()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun drawWeeklyChart() {
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        val goalLine = findViewById<View>(R.id.goalLine)
        chartContainer.removeAllViews()

        // 아직 걷지 않았으므로 전부 0으로 초기화 (테스트 시 0으로 정상 표시됨)
        val days = listOf("월", "화", "수", "목", "금", "토", "일")
        val weeklySteps = listOf(0, 0, 0, 0, 0, 0, 0) 
        
        val maxSteps = weeklySteps.maxOrNull()?.coerceAtLeast(dailyGoal) ?: dailyGoal
        val dp = resources.displayMetrics.density
        
        // 💡 버그 수정: 목표선이 차트 최대 높이(0.7)에 정확히 일치하도록 수식 변경
        val lineMarginTop = (204 * (1f - (0.7f * (dailyGoal.toFloat() / maxSteps.toFloat())))).toInt()
        val params = goalLine.layoutParams as FrameLayout.LayoutParams
        params.topMargin = (lineMarginTop * dp).toInt()
        goalLine.layoutParams = params

        for (i in days.indices) {
            val steps = weeklySteps[i]
            val isGoalReached = steps >= dailyGoal && steps > 0

            val dayLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            // 달성 왕관
            val tvCrown = TextView(this).apply {
                text = "👑"
                textSize = 14f
                gravity = Gravity.CENTER
                alpha = if (isGoalReached) 1f else 0f
            }

            // 막대 위 걸음 수 표기
            val tvSteps = TextView(this).apply {
                text = steps.toString()
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
            }

            // 💡 버그 수정: 막대 두께를 기기 해상도에 맞춰 일정하게(20dp) 설정
            val barHeightWeight = (steps.toFloat() / maxSteps.toFloat()) * 0.7f
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f - barHeightWeight)
            }
            val barWidth = (20 * dp).toInt()
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(barWidth, 0, barHeightWeight)
                setBackgroundColor(if (isGoalReached) Color.parseColor("#4CAF50") else Color.parseColor("#90CAF9"))
            }

            // 하단 요일 표기
            val tvDay = TextView(this).apply {
                text = days[i]
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }

            dayLayout.addView(spacer)
            dayLayout.addView(tvCrown)
            dayLayout.addView(tvSteps)
            dayLayout.addView(bar)
            dayLayout.addView(tvDay)

            chartContainer.addView(dayLayout)
        }
    }
}
