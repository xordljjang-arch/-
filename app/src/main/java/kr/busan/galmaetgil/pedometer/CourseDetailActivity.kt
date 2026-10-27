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

class CourseDetailActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mapView: MapView
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private lateinit var locationManager: LocationManager
    
    private lateinit var courseId: String
    private var checkpoints: List<Point> = emptyList()
    private lateinit var tvNextTarget: TextView

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
            Point("7. 부산문화회관", 35.12930, 129.09550),
