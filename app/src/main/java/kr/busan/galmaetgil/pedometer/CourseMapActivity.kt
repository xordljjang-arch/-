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
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
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

        val courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        
        val startLat = if (courseId == "1-1") 35.3195 else if (courseId == "2-1") 35.1593 else 35.1054
        val startLng = if (courseId == "1-1") 129.2750 else if (courseId == "2-1") 129.1758 else 129.1114

        val pathCoords = mutableListOf<LatLng>()
        for (i in 0 until 20) {
            val latLng = LatLng(startLat - (i * 0.002), startLng + (i * 0.002))
            pathCoords.add(latLng)
            
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

        naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(startLat, startLng)))
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
