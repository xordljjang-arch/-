package kr.busan.galmaetgil.pedometer

import android.location.Location

/**
 * 부산 갈맷길 트래킹 코스 GPS 매칭 및 70% 완주 도장 판별기
 *
 * 각 갈맷길 구간의 대표 GPS 체크포인트(위도, 경도) 목록을 보유하고,
 * 어르신이 걸으실 때 반경 50m 이내로 통과한 체크포인트가 전체의 70% 이상이면
 * 해당 갈맷길 구간의 완주 도장(스탬프)을 자동 발급합니다.
 */
class GalmaetgilGpsTracker {

    data class LatLngPoint(val lat: Double, val lng: Double, var passed: Boolean = false)

    // 부산 갈맷길 대표 코스 체크포인트 샘플 (2-1구간: 송정해수욕장 ~ 청사포 ~ 해운대 ~ 동백섬 ~ 민락교)
    private val courseWaypoints = mapOf(
        "2-1" to listOf(
            LatLngPoint(35.1786, 129.1996), // 송정해수욕장 시점
            LatLngPoint(35.1691, 129.1965), // 구덕포
            LatLngPoint(35.1613, 129.1903), // 청사포 다릿돌전망대
            LatLngPoint(35.1578, 129.1795), // 해운대 달맞이길 해월정
            LatLngPoint(35.1587, 129.1604), // 해운대해수욕장
            LatLngPoint(35.1524, 129.1536), // 동백섬 누리마루
            LatLngPoint(35.1565, 129.1422), // 수영만 요트경기장
            LatLngPoint(35.1643, 129.1318)  // 민락교 종점
        ),
        "1-2" to listOf(
            LatLngPoint(35.2445, 129.2223), // 기장군청
            LatLngPoint(35.2201, 129.2285), // 대변항
            LatLngPoint(35.2054, 129.2271), // 오랑대공원
            LatLngPoint(35.1884, 129.2233), // 해동용궁사
            LatLngPoint(35.1786, 129.1996)  // 송정해수욕장
        ),
        "2-2" to listOf(
            LatLngPoint(35.1643, 129.1318), // 민락교
            LatLngPoint(35.1532, 129.1186), // 광안리해수욕장
            LatLngPoint(35.1388, 129.1235), // 동생말 전망대
            LatLngPoint(35.1252, 129.1294), // 이기대 어울마당
            LatLngPoint(35.1018, 129.1263)  // 오륙도 해맞이공원
        ),
        "4-1" to listOf(
            LatLngPoint(35.0789, 129.0452), // 영도 절영해안산책로
            LatLngPoint(35.0758, 129.0185), // 송도해수욕장
            LatLngPoint(35.0612, 129.0156), // 암남공원
            LatLngPoint(35.0684, 129.0021)  // 감천항
        )
    )

    data class CourseEvaluation(
        val courseId: String,
        val progressPercent: Int,
        val isCompleted70Percent: Boolean
    )

    fun onLocationChanged(courseId: String, currentLat: Double, currentLng: Double): CourseEvaluation {
        val points = courseWaypoints[courseId] ?: return CourseEvaluation(courseId, 0, false)
        val results = FloatArray(1)

        for (pt in points) {
            if (!pt.passed) {
                Location.distanceBetween(currentLat, currentLng, pt.lat, pt.lng, results)
                // 체크포인트 반경 50m 이내 진입 시 통과 인정
                if (results[0] <= 50.0f) {
                    pt.passed = true
                }
            }
        }

        val passedCount = points.count { it.passed }
        val progressPct = ((passedCount.toDouble() / points.size.toDouble()) * 100.0).toInt()
        // 70% 이상 걸었을 때 완주 도장 발급!
        return CourseEvaluation(
            courseId = courseId,
            progressPercent = progressPct,
            isCompleted70Percent = progressPct >= 70
        )
    }
}
