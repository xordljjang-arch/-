private fun updateWeeklyChartUI() {
        val layoutWeeklyChart = findViewById<LinearLayout>(R.id.layoutWeeklyChart)
        layoutWeeklyChart?.removeAllViews()
        layoutWeeklyChart?.orientation = LinearLayout.VERTICAL // 차트 방향을 세로로 변경

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("MM/dd (E)", Locale.KOREAN) 
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val dateStr = format.format(cal.time)
            val dayStr = dayFormat.format(cal.time)
            val steps = if (i == 6) currentSteps else prefs.getInt("HISTORY_$dateStr", 0)

            // 가로 1줄(날짜 + 막대바 + 걸음수) 컨테이너
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, 16, 0, 16)
                gravity = Gravity.CENTER_VERTICAL
            }

            // 1. 날짜 (예: 10/27 (화))
            val tvDay = TextView(this).apply {
                text = dayStr
                textSize = 13f
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            }

            // 2. 가로 막대 그래프 (목표치 대비 달성률)
            val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
                max = stepGoal 
                progress = steps
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                }
            }

            // 3. 걸음 수 텍스트
            val tvSteps = TextView(this).apply {
                text = "${steps}보"
                textSize = 13f
                setTextColor(Color.parseColor("#2E7D32"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                setPadding(16, 0, 0, 0)
            }

            row.addView(tvDay)
            row.addView(progressBar)
            row.addView(tvSteps)
            layoutWeeklyChart?.addView(row)

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
