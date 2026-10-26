package kr.busan.galmaetgil.pedometer

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StampActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stamp)

        val tvNearest = findViewById<TextView>(R.id.tvNearestCourse)
        val tracker = GalmaetgilGpsTracker(this)
        
        val courses = mapOf(
            "1-1" to Pair(35.3195, 129.2750),
            "2-1" to Pair(35.1593, 129.1758),
            "3-1" to Pair(35.1054, 129.1114)
        )

        if (tracker.canGetLocation()) {
            val myLoc = Location("").apply { latitude = tracker.latitude; longitude = tracker.longitude }
            var nearestName = ""
            var minDistance = Float.MAX_VALUE

            for ((name, coords) in courses) {
                val courseLoc = Location("").apply { latitude = coords.first; longitude = coords.second }
                val dist = myLoc.distanceTo(courseLoc)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestName = name
                }
            }
            tvNearest.text = "📍 현재 위치에서 가장 가까운 코스: ${nearestName}구간"
        }

        findViewById<Button>(R.id.btnCourse1).setOnClickListener { openMap("1-1") }
        findViewById<Button>(R.id.btnCourse2).setOnClickListener { openMap("2-1") }
        findViewById<Button>(R.id.btnCourse3).setOnClickListener { openMap("3-1") }
    }

    private fun openMap(courseId: String) {
        val intent = Intent(this, CourseMapActivity::class.java)
        intent.putExtra("COURSE_ID", courseId)
        startActivity(intent)
    }
}
