package kr.busan.galmaetgil.pedometer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StampActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListUI()
    }

    override fun onResume() {
        super.onResume()
        setupListUI() // 도장을 찍고 돌아오면 새로고침
    }

    private fun setupListUI() {
        val prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        // 상단 배너
        val topBanner = TextView(this).apply {
            text = "📍 현재 위치에서 가장 가까운 코스: 2-1구간"
            textSize = 16f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#FFCDD2"))
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 48)
            }
        }
        layout.addView(topBanner)

        // 각 코스별 10개 완주 여부 확인 및 버튼 생성
        val courses = listOf(
            Triple("1-1", "1-1구간 (임랑해수욕장 ~ 기장군청)\n거리: 12.2KM | 난이도: 쉬움", 10),
            Triple("2-1", "2-1구간 (문탠로드 ~ 오륙도)\n거리: 5.7KM | 난이도: 보통", 10),
            Triple("3-1", "3-1구간 (오륙도 ~ 태종대)\n거리: 11.5KM | 난이도: 어려움", 10)
        )

        for (c in courses) {
            var isCompleted = true
            for (i in 0 until c.third) {
                if (!prefs.getBoolean("CP_${c.first.replace("-", "_")}_$i", false)) {
                    isCompleted = false
                    break
                }
            }

            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                setPadding(32, 48, 32, 48)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 32)
                }
                setOnClickListener {
                    startActivity(Intent(this@StampActivity, CourseDetailActivity::class.java).apply {
                        putExtra("COURSE_ID", c.first)
                    })
                }

                val tvTitle = TextView(this@StampActivity).apply {
                    text = if (isCompleted) "🏆 [완주 완료]\n${c.second}" else c.second
                    textSize = 15f
                    setTextColor(if (isCompleted) Color.parseColor("#2E7D32") else Color.BLACK)
                    setTypeface(null, if (isCompleted) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                    gravity = Gravity.CENTER
                }
                addView(tvTitle)
            }
            layout.addView(btn)
        }

        setContentView(layout)
    }
}
