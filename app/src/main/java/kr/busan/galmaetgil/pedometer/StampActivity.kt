package kr.busan.galmaetgil.pedometer

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StampActivity : AppCompatActivity() {

    private lateinit var tvStampStatus: TextView
    private lateinit var gridCheckpoints: GridView
    private lateinit var btnCheckLocation: Button
    private lateinit var gpsTracker: GalmaetgilGpsTracker

    // 20개의 가상 체크포인트 좌표
    private val checkpoints = List(20) { index -> 
        Pair(35.1500 + (index * 0.005), 129.1000 + (index * 0.005)) 
    }
    private val checkedStatus = BooleanArray(20) { false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stamp)

        tvStampStatus = findViewById(R.id.tvStampStatus)
        gridCheckpoints = findViewById(R.id.gridCheckpoints)
        btnCheckLocation = findViewById(R.id.btnCheckLocation)
        
        gpsTracker = GalmaetgilGpsTracker(this)
        
        gridCheckpoints.adapter = CheckpointAdapter()

        btnCheckLocation.setOnClickListener {
            verifyCheckpoints()
        }
    }

    private fun verifyCheckpoints() {
        if (!gpsTracker.canGetLocation()) {
            Toast.makeText(this, "GPS 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val myLat = gpsTracker.latitude
        val myLon = gpsTracker.longitude
        val myLoc = Location("").apply { latitude = myLat; longitude = myLon }

        var newChecked = 0
        for (i in checkpoints.indices) {
            if (!checkedStatus[i]) {
                val pointLoc = Location("").apply { 
                    latitude = checkpoints[i].first
                    longitude = checkpoints[i].second 
                }
                
                if (myLoc.distanceTo(pointLoc) <= 500f) {
                    checkedStatus[i] = true
                    newChecked++
                }
            }
        }

        val totalChecked = checkedStatus.count { it }
        
        if (totalChecked >= 14) {
            tvStampStatus.text = "현재 달성: $totalChecked / 20 개\n🎉 축하합니다! 갈맷길 도장 획득 완료!"
            tvStampStatus.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            tvStampStatus.text = "현재 달성: $totalChecked / 20 개 (14개 이상 도장 발급)"
        }

        (gridCheckpoints.adapter as CheckpointAdapter).notifyDataSetChanged()
        
        if (newChecked > 0) {
            Toast.makeText(this, "${newChecked}개의 새 포인트를 통과했습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "반경 500m 이내의 새 포인트가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    inner class CheckpointAdapter : BaseAdapter() {
        override fun getCount(): Int = 20
        override fun getItem(p0: Int): Any = checkedStatus[p0]
        override fun getItemId(p0: Int): Long = p0.toLong()
        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = TextView(this@StampActivity)
            tv.text = "${pos + 1}번"
            tv.textSize = 14f
            tv.gravity = android.view.Gravity.CENTER
            tv.setPadding(10, 20, 10, 20)
            
            // 오류가 발생했던 텍스트 색상 설정 코드 수정 완료
            if (checkedStatus[pos]) {
                tv.setBackgroundColor(Color.parseColor("#4CAF50"))
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundColor(Color.parseColor("#E0E0E0"))
                tv.setTextColor(Color.BLACK)
            }
            return tv
        }
    }
}
