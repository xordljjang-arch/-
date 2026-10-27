package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class CourseDetailActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var courseId: String
    private var checkpoints: List<Point> = emptyList()

    private lateinit var tvMyLocation: TextView
    private lateinit var tvNextTarget: TextView

    // ★ 공식 데이터 교차 검증: 완벽한 경로상 정밀 좌표 10개씩 배치
    private val courseData = mapOf(
        "1-1" to listOf(
            Point("1. 임랑해수욕장 (시작)", 35.31900, 129.27702),
            Point("2. 칠암항 (야구등대)", 35.30235, 129.26180),
            Point("3. 신평소공원", 35.29360, 129.25595),
            Point("4. 동백항", 35.28420, 129.25190),
            Point("5. 이동항", 35.27555, 129.24580),
            Point("6. 일광해수욕장 (북측)", 35.26380, 129.23725),
            Point("7. 일광해수욕장 (남측)", 35.25850, 129.23350),
            Point("8. 기장체육관 방면", 35.25050, 129.22450),
            Point("9. 죽성리 교차로", 35.24750, 129.22200),
            Point("10. 기장군청 (종점)", 35.24460, 129.22236)
        ),
        "2-1" to listOf(
            Point("1. 문탠로드 (시작)", 35.16105, 129.17185),
            Point("2. 달맞이 어울림마당", 35.15850, 129.17000),
            Point("3. 해운대해수욕장 (미포)", 35.15920, 129.16780),
            Point("4. 해운대해수욕장 (중앙)", 35.15870, 129.16040),
            Point("5. 해운대 조선비치", 35.15780, 129.15390),
            Point("6. 동백섬 누리마루", 35.15240, 129.15340),
            Point("7. 동백섬 출렁다리", 35.15330, 129.15110),
            Point("8. 마린시티 영화의거리", 35.15540, 129.14440),
            Point("9. 요트경기장 광장", 35.16100, 129.14150),
            Point("10. 민락교 (종점)", 35.16330, 129.13500)
        ),
        "3-1" to listOf(
            Point("1. 오륙도 스카이워크 (시작)", 35.10580, 129.12320),
            Point("2. 백운포 체육공원", 35.10540, 129.11210),
            Point("3. 신선대 섶자리", 35.10180, 129.10370),
            Point("4. 신선대 유원지", 35.10510, 129.10090),
            Point("5. 평화공원", 35.12780, 129.09630),
            Point("6. UN기념공원", 35.12770, 129.09880),
            Point("7. 부산문화회관", 35.12930, 129.09550),
            Point("8. 우암동 소막마을", 35.13250, 129.07580),
            Point("9. 자성대공원", 35.13620, 129.06260),
            Point("10. 부산진시장 (종점)", 35.13680, 129.05920)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        checkpoints = courseData[courseId] ?: emptyList()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F9FA"))
        }

        // ★ 실시간 내 위치 대시보드
        val dashboard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#E3F2FD")) // 연한 파란색 바탕
        }
        
        tvMyLocation = TextView(this).apply { 
            text = "🛰️ 내 실시간 위치: GPS 수신 대기 중..." 
            textSize = 14f
            setTextColor(Color.parseColor("#1565C0"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0,0,0,16)
        }
        tvNextTarget = TextView(this).apply { 
            text = "🎯 다음 목표: 대기 중..." 
            textSize = 15f
            setTextColor(Color.parseColor("#D32F2F"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        dashboard.addView(tvMyLocation)
        dashboard.addView(tvNextTarget)
        rootLayout.addView(dashboard)

        // 체크포인트 목록
        val scrollView = ScrollView(this)
        val listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 48)
        }

        val title = TextView(this).apply {
            text = "🚩 $courseId 구간 체크포인트\n(500m 이내 진입 시 자동 체크)"
            textSize = 17f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 32)
        }
        listLayout.addView(title)

        for ((index, cp) in checkpoints.withIndex()) {
            val cpView = TextView(this).apply {
                id = index + 1000
                text = "⏳ ${cp.name}"
                textSize = 14f
                setPadding(0, 24, 0, 24)
            }
            listLayout.addView(cpView)
        }

        scrollView.addView(listLayout)
        rootLayout.addView(scrollView)
        setContentView(rootLayout)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startLocationTracking()
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2f, this)
    }

    override fun onLocationChanged(location: Location) {
        val prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        
        // 1. 내 위치 실시간 출력
        tvMyLocation.text = "🛰️ 내 위치: 위도 ${String.format("%.4f", location.latitude)}, 경도 ${String.format("%.4f", location.longitude)}"

        var nextTargetName = "모든 체크포인트 통과 완료!"
        var nextTargetDistance = 0f

        // 2. 체크포인트 10개 검사
        for ((index, cp) in checkpoints.withIndex()) {
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, cp.lat, cp.lng, results)
            val distance = results[0]
            
            val prefKey = "CP_${courseId.replace("-", "_")}_$index"
            val isChecked = prefs.getBoolean(prefKey, false)

            if (!isChecked && distance <= 500) {
                prefs.edit().putBoolean(prefKey, true).apply()
                Toast.makeText(this, "✅ ${cp.name} 통과!", Toast.LENGTH_SHORT).show()
            }

            val view = findViewById<TextView>(index + 1000)
            if (prefs.getBoolean(prefKey, false)) {
                view?.text = "✅ ${cp.name} (통과 완료)"
                view?.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                view?.text = "❌ ${cp.name} (현재 거리: ${distance.toInt()}m 남음)"
                view?.setTextColor(Color.DKGRAY)
                
                // 아직 통과 안 한 가장 가까운(또는 다음) 목표 찾기
                if (nextTargetName == "모든 체크포인트 통과 완료!") {
                    nextTargetName = cp.name
                    nextTargetDistance = distance
                }
            }
        }
        
        // 3. 다음 목표 대시보드 업데이트
        if (nextTargetName != "모든 체크포인트 통과 완료!") {
            tvNextTarget.text = "🎯 다음 목표: $nextTargetName (${nextTargetDistance.toInt()}m 남음)"
        } else {
            tvNextTarget.text = "🏆 $nextTargetName"
            tvNextTarget.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }

    data class Point(val name: String, val lat: Double, val lng: Double)
}
