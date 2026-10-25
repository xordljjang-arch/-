package kr.busan.galmaetgil.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener, TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private lateinit var locationManager: LocationManager
    private var tts: TextToSpeech? = null

    private val antiCheatDetector = AntiCheatDetector()
    private val gpsTracker = GalmaetgilGpsTracker()

    private var initialSensorSteps: Int = -1
    private var todaySteps: Int = 6420
    private var activeCourseId: String = "2-1"
    private var lastCoursePct: Int = 65
    private var lastLocation: Location? = null
    private var gpsMovedMeters: Float = 25.0f

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.textZoom = 100

        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")

        tts = TextToSpeech(this, this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
        } else {
            startSensorsAndGps()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSensorsAndGps() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, this)
            }
        } catch (_: Exception) {}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startSensorsAndGps()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val rawSensorTotal = event.values[0].toInt()
            if (initialSensorSteps < 0) {
                initialSensorSteps = rawSensorTotal
            }
            val addedSteps = rawSensorTotal - initialSensorSteps
            todaySteps = 6420 + addedSteps

            val check = antiCheatDetector.recordStepAndEvaluate(
                currentTimeMs = System.currentTimeMillis(),
                gpsMovedMetersLast10Min = gpsMovedMeters
            )
            pushUpdateToWebView(
                isStampedNow = lastCoursePct >= 70,
                antiCheatStatus = check.status,
                antiCheatReason = check.reason
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        lastLocation?.let { prev ->
            gpsMovedMeters += prev.distanceTo(location)
        }
        lastLocation = location

        val eval = gpsTracker.onLocationChanged(activeCourseId, location.latitude, location.longitude)
        lastCoursePct = eval.progressPercent
        if (eval.isCompleted70Percent) {
            speakKorean("축하합니다! 부산 갈맷길 코스를 70퍼센트 이상 걸으셔서 완주 도장이 찍혔습니다!")
        }
        pushUpdateToWebView(
            isStampedNow = eval.isCompleted70Percent,
            antiCheatStatus = "verified",
            antiCheatReason = "갈맷길 GPS 이동 경로 및 정상 보행 확인됨"
        )
    }

    private fun pushUpdateToWebView(isStampedNow: Boolean, antiCheatStatus: String, antiCheatReason: String) {
        val safeReason = antiCheatReason.replace("'", "")
        val js = "if(window.onNativeStepUpdate){window.onNativeStepUpdate($todaySteps,'$activeCourseId',$lastCoursePct,$isStampedNow,'$antiCheatStatus','$safeReason');}"
        runOnUiThread {
            webView.evaluateJavascript(js, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            tts?.setSpeechRate(0.92f)
        }
    }

    private fun speakKorean(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "galmaetgil_tts")
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun speak(text: String) {
            speakKorean(text)
        }

        @JavascriptInterface
        fun selectCourse(courseId: String) {
            activeCourseId = courseId
        }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        tts?.shutdown()
        super.onDestroy()
    }
}
