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
        
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        
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

    private fun getCourseCoordinates(courseId: String): List<LatLng> {
        return when (courseId) {
            "1-1" -> listOf( 
                LatLng(35.3188, 129.2748), LatLng(35.3115, 129.2700), LatLng(35.3025, 129.2618),
                LatLng(35.2970, 129.2590), LatLng(35.2925, 129.2568), LatLng(35.2870, 129.2550),
                LatLng(35.2810, 129.2520), LatLng(35.2750, 129.2480), LatLng(35.2700, 129.2440),
                LatLng(35.2650, 129.2400), LatLng(35.2605, 129.2345), LatLng(35.2570, 129.2320),
                LatLng(35.2540, 129.2290), LatLng(35.2510, 129.2260), LatLng(35.2485, 129.2245),
                LatLng(35.2465, 129.2235), LatLng(35.2450, 129.2228), LatLng(35.2442, 129.2225),
                LatLng(35.2438, 129.2223), LatLng(35.2436, 129.2222)
            )
            "2-1" -> listOf( 
                LatLng(35.1596, 129.1755), LatLng(35.1595, 129.1710), LatLng(35.1590, 129.1660),
                LatLng(35.1585, 129.1610), LatLng(35.1578, 129.1560), LatLng(35.1565, 129.1541),
                LatLng(35.1526, 129.1534), LatLng(35.1540, 129.1525), LatLng(35.1565, 129.1520), 
                LatLng(35.1555, 129.1490), LatLng(35.1548, 129.1465), LatLng(35.1560, 129.1440), 
                LatLng(35.1580, 129.1430), LatLng(35.1591, 129.1415), LatLng(35.1610, 129.1380), 
                LatLng(35.1620, 129.1350), LatLng(35.1630, 129.1320), LatLng(35.1640, 129.1305), 
                LatLng(35.1645, 129.1298), LatLng(35.1650, 129.1293)
            )
            "3-1" -> listOf( 
                LatLng(35.1054, 129.1114), LatLng(35.1070, 129.1100), LatLng(35.1090, 129.1060),
                LatLng(35.1110, 129.1020), LatLng(35.1140, 129.1000), LatLng(35.1180, 129.0980),
                LatLng(35.1230, 129.0970), LatLng(35.1275, 129.0965), LatLng(35.1285, 129.0930), 
                LatLng(35.1290, 129.0890), LatLng(35.1280, 129.0850), LatLng(35.1275, 129.0810), 
                LatLng(35.1285, 129.0760), LatLng(35.1300, 129.0720), LatLng(35.1320, 129.0680), 
                LatLng(35.1340, 129.0640), LatLng(35.1360, 129.0610), LatLng(35.1370, 129.0590), 
                LatLng(35.1380, 129.0580), LatLng(35.1385, 129.0595)
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
