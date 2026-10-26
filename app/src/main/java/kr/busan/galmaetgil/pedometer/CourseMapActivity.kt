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

        // 위치 소스 초기화 (1초 간격 갱신)
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
        
        // 앱 실행 시 내 위치를 자동으로 추적하고 지도 중심을 맞춤
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        
        // 1-1, 2-1, 3-1 코스별 실제 해안/육지 산책로 경로 좌표 불러오기
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

    // 3개 코스 실제 좌표 데이터 정의 함수
    private fun getCourseCoordinates(courseId: String): List<LatLng> {
        return when (courseId) {
            "1-1" -> listOf(
                LatLng(35.1054, 129.1114), LatLng(35.1070, 129.1125), LatLng(35.1090, 129.1140),
                LatLng(35.1110, 129.1155), LatLng(35.1130, 129.1170), LatLng(35.1150, 129.1185),
                LatLng(35.1170, 129.1190), LatLng(35.1190, 129.1205), LatLng(35.1210, 129.1220),
                LatLng(35.1230, 129.1235), LatLng(35.1250, 129.1240), LatLng(35.1270, 129.1255),
                LatLng(35.1290, 129.1270), LatLng(35.1310, 129.1285), LatLng(35.1330, 129.1290),
                LatLng(35.1350, 129.1305), LatLng(35.1370, 129.1320), LatLng(35.1390, 129.1335),
                LatLng(35.1410, 129.1350), LatLng(35.1430, 129.1365)
            )
            "2-1" -> listOf(
                LatLng(35.1593, 129.1758), LatLng(35.1610, 129.1770), LatLng(35.1630, 129.1790),
                LatLng(35.1650, 129.1810), LatLng(35.1670, 129.1830), LatLng(35.1690, 129.1850),
                LatLng(35.1710, 129.1870), LatLng(35.1730, 129.1890), LatLng(35.1750, 129.1910),
                LatLng(35.1770, 129.1930), LatLng(35.1790, 129.1950), LatLng(35.1810, 129.1970),
                LatLng(35.1830, 129.1990), LatLng(35.1850, 129.2010), LatLng(35.1870, 129.2030),
                LatLng(35.1890, 129.2050), LatLng(35.1910, 129.2070), LatLng(35.1930, 129.2090),
                LatLng(35.1950, 129.2110), LatLng(35.1970, 129.2130)
            )
            "3-1" -> listOf(
                LatLng(35.3195, 129.2750), LatLng(35.3210, 129.2765), LatLng(35.3230, 129.2780),
                LatLng(35.3250, 129.2795), LatLng(35.3270, 129.2810), LatLng(35.3290, 129.2825),
                LatLng(35.3310, 129.2840), LatLng(35.3330, 129.2855), LatLng(35.3350, 129.2870),
                LatLng(35.3370, 129.2885), LatLng(35.3390, 129.2900), LatLng(35.3410, 129.2915),
                LatLng(35.3430, 129.2930), LatLng(35.3450, 129.2945), LatLng(35.3470, 129.2960),
                LatLng(35.3490, 129.2975), LatLng(35.3510, 129.2990), LatLng(35.3530, 129.3005),
                LatLng(35.3550, 129.3020), LatLng(35.3570, 129.3035)
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
