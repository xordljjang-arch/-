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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class CourseDetailActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mapView: MapView
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private lateinit var locationManager: LocationManager
    
    private lateinit var courseId: String
    private var checkpoints: List<Point> = emptyList()
    private lateinit var tvNextTarget: TextView
    private val markerList = mutableListOf<Marker>()
    private val pathOverlay = PathOverlay()

    private val CLIENT_ID = "nc59cc4zg0"
    private val CLIENT_SECRET = "여기에_네이버클라우드_Directions5_Client_Secret을_입력하세요" 

    private val courseData = mapOf(
        "1-1" to listOf(
            Point("1. 임랑해수욕장", 35.31900, 129.27702), Point("2. 칠암항", 35.30235, 129.26180),
            Point("3. 신평소공원", 35.29360, 129.25595), Point("4. 동백항", 35.28420, 129.25190),
            Point("5. 이동항", 35.27555, 129.24580), Point("6. 일광해수욕장 북측", 35.26380, 129.23725),
            Point("7. 일광해수욕장 남측", 35.25850, 129.23350), Point("8. 기장체육관 방면", 35.25050, 129.22450),
            Point("9. 죽성리 교차로", 35.24750, 129.22200), Point("10. 기장군청", 35.24460, 129.22236)
        ),
        "2-1" to listOf(
            Point("1. 문탠로드", 35.16105, 129.17185), Point("2. 달맞이 어울림마당", 35.15850, 129.17000),
            Point("3. 해운대 미포", 35.15920, 129.16780), Point("4. 해운대 중앙", 35.15870, 129.16040),
            Point("5. 조선비치", 35.15780, 129.15390), Point("6. 누리마루", 35.15240, 129.15340),
            Point("7. 출렁다리", 35.15330, 129.15110), Point("8. 영화의거리", 35.15540, 129.14440),
            Point("9. 요트경기장", 35.16100, 129.14150), Point("10. 민락교", 35.16330, 129.13500)
        ),
        "3-1" to listOf(
            Point("1. 오륙도 스카이워크", 35.10580, 129.12320), Point("2. 백운포", 35.10540, 129.11210),
            Point("3. 신선대 섶자리", 35.10180, 129.10370), Point("4. 신선대 유원지", 35.10510, 129.10090),
            Point("5. 평화공원", 35.12780, 129.09630), Point("6. UN기념공원", 35.12770, 129.09880),
            Point("7. 부산문화회관", 35.12930, 129.09550), Point("8. 소막마일", 35.13250, 129.07580),
            Point("9. 자성대공원", 35.13620, 129.06260), Point("10. 부산진시장", 35.13680, 129.05920)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(CLIENT_ID)

        courseId = intent.getStringExtra("COURSE_ID") ?: "1-1"
        checkpoints = courseData[courseId] ?: emptyList()

        val rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        tvNextTarget = TextView(this).apply {
            text = "🚩 $courseId 정밀 도보 경로 로딩 중..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            setPadding(32, 48, 32, 48)
        }
        rootLayout.addView(tvNextTarget)

        mapView = MapView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(mapView)
        setContentView(rootLayout)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        locationSource = FusedLocationSource(this, 100)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val prefs = getSharedPreferences("PedometerPrefs", Context.MODE_PRIVATE)
        markerList.clear()

        for ((index, cp) in checkpoints.withIndex()) {
            val position = LatLng(cp.lat, cp.lng)
            val prefKey = "CP_${courseId.replace("-", "_")}_$index"
            val isChecked = prefs.getBoolean(prefKey, false)

            val marker = Marker().apply {
                this.position = position
                captionText = cp.name
                iconTintColor = if (isChecked) Color.parseColor("#4CAF50") else Color.parseColor("#E53935")
                this.map = naverMap
            }
            markerList.add(marker)
        }

        if (checkpoints.isNotEmpty()) {
            val cameraUpdate = CameraUpdate.scrollTo(LatLng(checkpoints[0].lat, checkpoints[0].lng))
            naverMap.moveCamera(cameraUpdate)
        }

        // 인접 지점별 개별 순차 호출로 왜곡 없는 완벽한 곡선 경로 구축
        fetchWalkingRoute()

        startLocationTracking()
    }

    private fun fetchWalkingRoute() {
        lifecycleScope.launch(Dispatchers.IO) {
            val fullRouteCoords = mutableListOf<LatLng>()
            val client = OkHttpClient()

            for (i in 0 until checkpoints.size - 1) {
                val start = checkpoints[i]
                val goal = checkpoints[i + 1]

                val url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start=${start.lng},${start.lat}&goal=${goal.lng},${goal.lat}&option=traoptimal"

                try {
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("X-NCP-APIGW-API-KEY-ID", CLIENT_ID)
                        .addHeader("X-NCP-APIGW-API-KEY", CLIENT_SECRET)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val coords = parsePathFromJson(response.body?.string())
                        if (coords.isNotEmpty()) {
                            fullRouteCoords.addAll(coords)
                        } else {
                            fullRouteCoords.add(LatLng(start.lat, start.lng))
                            fullRouteCoords.add(LatLng(goal.lat, goal.lng))
                        }
                    } else {
                        fullRouteCoords.add(LatLng(start.lat, start.lng))
                        fullRouteCoords.add(LatLng(goal.lat, goal.lng))
                    }
                } catch (e: Exception) {
                    fullRouteCoords.add(LatLng(start.lat, start.lng))
                    fullRouteCoords.add(LatLng(goal.lat, goal.lng))
                }
            }

            withContext(Dispatchers.Main) {
                if (fullRouteCoords.size >= 2) {
                    pathOverlay.coords = fullRouteCoords
                    pathOverlay.color = Color.parseColor("#1976D2")
                    pathOverlay.width = 16
                    pathOverlay.outlineWidth = 3
                    pathOverlay.outlineColor = Color.WHITE
                    pathOverlay.map = naverMap
                }
                tvNextTarget.text = "🚩 $courseId 구간 정밀 도보 탐방 시작"
            }
        }
    }

    private fun parsePathFromJson(json: String?): List<LatLng> {
        val list = mutableListOf<LatLng>()
        if (json == null) return list
        try {
            val gson = Gson()
            val map = gson.fromJson(json, Map::class.java)
            val route = map["route"] as? Map<*, *>
            val traoptimal = route?.get("traoptimal") as? List<*>
            if (!traoptimal.isNullOrEmpty()) {
                val firstPath = traoptimal[0] as? Map<*, *>
                val path = firstPath?.get("path") as? List<*>
                if (path != null) {
                    for (item in path) {
                        if (item is List<*> && item.size >= 2) {
                            val lng = (item[0] as Number).toDouble()
                            val lat = (item[1] as Number).toDouble()
                            list.add(LatLng(lat, lng))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
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
        var nextTargetName = "모든 지점 통과 완료!"
        var nextTargetDistance = 0f

        for ((index, cp) in checkpoints.withIndex()) {
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, cp.lat, cp.lng, results)
            val distance = results[0] 
            val prefKey = "CP_${courseId.replace("-", "_")}_$index"
            val isChecked = prefs.getBoolean(prefKey, false)

            if (!isChecked && distance <= 40f) {
                prefs.edit().putBoolean(prefKey, true).apply()
                if (index < markerList.size) {
                    markerList[index].iconTintColor = Color.parseColor("#4CAF50")
                }
                Toast.makeText(this, "🎉 [인증 완료] ${cp.name} 도달!", Toast.LENGTH_SHORT).show()
            }

            if (!prefs.getBoolean(prefKey, false) && nextTargetName == "모든 지점 통과 완료!") {
                nextTargetName = cp.name
                nextTargetDistance = distance
            }
        }

        tvNextTarget.text = if (nextTargetName == "모든 지점 통과 완료!") {
            "🏆 부산 갈맷길 $courseId 구간 완주 성공!"
        } else {
            "🎯 다음 도보 목표: $nextTargetName (${nextTargetDistance.toInt()}m 남음)"
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy(); locationManager.removeUpdates(this) }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}

data class Point(val name: String, val lat: Double, val lng: Double)
