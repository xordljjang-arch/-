package kr.busan.galmaetgil.pedometer

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.util.FusedLocationSource

class CourseMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap
    private val checkpoints = mutableListOf<Marker>()
    private val checkedStatus = BooleanArray(20) { false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 네이버 지도 SDK 3.24.0 신규 인증 규격 강제 주입
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient("nc59cc4zg0")
        setContentView(R.layout.activity_course_map)

        locationSource = FusedLocationSource(this, 1000)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.btnCheckStamp).setOnClickListener {
            verifyLocation()
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true 
        
        // 앱 실행 시 내 위치를 실시간으로 추적 및 표시
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        
        // 각 코스별 실제 해안/육지 경로 좌표 불러오기
        val pathCoords = getCourseCoordinates(courseId)

        for (i in pathCoords.indices) {
            val latLng = pathCoords[i]
            val marker = Marker()
            marker.position = latLng
            marker.captionText = "${i+1}번"
            marker.iconTintColor = Color.GRAY 
            marker.map = naverMap
            checkpoints.add(marker)
        }

        val polyline = PolylineOverlay()
        polyline.coords = pathCoords
        polyline.color = Color.parseColor("#0277BD")
        polyline.width = 15
        polyline.map = naverMap

        if (pathCoords.isNotEmpty()) {
            naverMap.moveCamera(CameraUpdate.scrollTo(pathCoords[0]))
        }
    }

    // 1-1, 2-1, 3-1 코스별 실제 지형 맞춤형 좌표 데이터
    private fun getCourseCoordinates(courseId: String): List<LatLng> {
        return when (courseId) {
            "1-1" -> listOf( // 임랑해수욕장 ~ 기장군청 구간
                LatLng(35.3267, 129.2818), LatLng(35.3200, 129.2800), LatLng(35.3130, 129.2780),
                LatLng(35.3060, 129.2750), LatLng(35.2990, 129.2720), LatLng(35.2920, 129.2680),
                LatLng(35.2850, 129.2640), LatLng(35.2780, 129.2600), LatLng(35.2710, 129.2550),
                LatLng(35.2640, 129.2500), LatLng(35.2580, 129.2450), LatLng(35.2520, 129.2400),
                LatLng(35.2460, 129.2350), LatLng(35.2410, 129.2310), LatLng(35.2360, 129.2280),
                LatLng(35.2320, 129.2250), LatLng(35.2280, 129.2230), LatLng(35.2250, 129.2215),
                LatLng(35.2220, 129.2205), LatLng(35.2200, 129.2200)
            )
            "2-1" -> listOf( // 미포 ~ 해운대 ~ 민락교 구간
                LatLng(35.1610, 129.1670), LatLng(35.1600, 129.1640), LatLng(35.1590, 129.1610),
                LatLng(35.1585, 129.1580), LatLng(35.1580, 129.1550), LatLng(35.1575, 129.1520),
                LatLng(35.1565, 129.1500), LatLng(35.1555, 129.1525), LatLng(35.1550, 129.1550),
                LatLng(35.1545, 129.1520), LatLng(35.1540, 129.1490), LatLng(35.1542, 129.1460),
                LatLng(35.1545, 129.1430), LatLng(35.1550, 129.1400), LatLng(35.1565, 129.1375),
                LatLng(35.1580, 129.1350), LatLng(35.1595, 129.1320), LatLng(35.1610, 129.1295),
                LatLng(35.1625, 129.1275), LatLng(35.1640, 129.1260)
            )
            "3-1" -> listOf( // 오륙도 ~ 신선대 ~ 부산진시장 구간
                LatLng(35.0936, 129.1175), LatLng(35.0980, 129.1150), LatLng(35.1020, 129.1130),
                LatLng(35.1060, 129.1100), LatLng(35.1100, 129.1070), LatLng(35.1130, 129.1040),
                LatLng(35.1160, 129.1010), LatLng(35.1180, 129.0980), LatLng(35.1200, 129.0950),
                LatLng(35.1220, 129.0920), LatLng(35.1245, 129.0890), LatLng(35.1265, 129.0860),
                LatLng(35.1285, 129.0830), LatLng(35.1300, 129.0800), LatLng(35.1315, 129.0765),
                LatLng(35.1330, 129.0730), LatLng(35.1345, 129.0695), LatLng(35.1360, 129.0660),
                LatLng(35.1370, 129.0630), LatLng(35.1380, 129.0605)
            )
            else -> listOf(LatLng(35.1054, 129.1114))
        }
    }

    private fun verifyLocation() {
        val lastLocation = locationSource.lastLocation
        if (lastLocation == null) {
            Toast.makeText(this, "GPS 위치를 잡는 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val myLoc = Location("").apply { latitude = lastLocation.latitude; longitude = lastLocation.longitude }
        var newChecked = 0

        for (i in checkpoints.indices) {
            if (!checkedStatus[i]) {
                val marker = checkpoints[i]
                val pointLoc = Location("").apply { latitude = marker.position.latitude; longitude = marker.position.longitude }
                
                if (myLoc.distanceTo(pointLoc) <= 500f) {
                    checkedStatus[i] = true
                    marker.iconTintColor = Color.GREEN
                    newChecked++
                }
            }
        }

        val totalChecked = checkedStatus.count { it }
        val tvStatus = findViewById<TextView>(R.id.tvMapStatus)
        
        if (totalChecked >= 14) {
            tvStatus.text = "달성률: $totalChecked / 20 🎉 도장 획득 완료!"
            tvStatus.setTextColor(Color.RED)
        } else {
            tvStatus.text = "달성률: $totalChecked / 20 (14개 이상 도장 발급)"
        }
        
        Toast.makeText(this, "${newChecked}개 포인트 도장 쾅!", Toast.LENGTH_SHORT).show()
    }
}
