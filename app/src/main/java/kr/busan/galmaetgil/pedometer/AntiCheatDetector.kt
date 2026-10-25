package kr.busan.galmaetgil.pedometer

import java.util.Calendar
import kotlin.math.sqrt

/**
 * 어르신 만보기 공정 순위를 위한 부정행위(자동걷기 장비 / 심야 이상 패턴) 판별기
 *
 * 1) 무휴식 정속 패턴 감지:
 *    자동걷기 장비(진자 흔들기 기계)는 보폭 시간 간격(Step Interval)의 표준편차가 극도로 낮고(CV < 2.5%),
 *    쉬는 구간 없이 1~2만 보를 똑같은 박자로 연속 기록합니다.
 * 2) 심야 비현실적 시간대(01:00 ~ 05:00) 대량 걸음 감지:
 *    새벽 시간대에 GPS 위치 이동 없이 집중된 걸음을 걸러냅니다.
 */
class AntiCheatDetector {

    private val stepTimestampsMs = ArrayDeque<Long>()
    private var continuousUnbrokenSteps = 0
    private var lastStepTimeMs = 0L
    private var nightSuspiciousSteps = 0

    data class VerificationResult(
        val status: String, // "verified", "flagged_machine", "flagged_night"
        val reason: String
    )

    fun recordStepAndEvaluate(currentTimeMs: Long, gpsMovedMetersLast10Min: Float): VerificationResult {
        if (lastStepTimeMs > 0L) {
            val deltaMs = currentTimeMs - lastStepTimeMs
            // 60초 이상 멈추면 '자연스러운 휴식'으로 인정하여 무휴식 연속 카운트 초기화
            if (deltaMs > 60_000L) {
                continuousUnbrokenSteps = 0
            } else {
                continuousUnbrokenSteps++
            }
        }
        lastStepTimeMs = currentTimeMs

        stepTimestampsMs.addLast(currentTimeMs)
        if (stepTimestampsMs.size > 120) {
            stepTimestampsMs.removeFirst()
        }

        // 1. 심야 비현실 시간대(새벽 1시 ~ 5시) + 제자리 흔들기 체크
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 1..4 && gpsMovedMetersLast10Min < 15f) {
            nightSuspiciousSteps++
            if (nightSuspiciousSteps >= 500) {
                return VerificationResult(
                    status = "flagged_night",
                    reason = "심야 시간대(01~05시) 제자리 비정상 집중 걸음 감지 (공정 순위 제외)"
                )
            }
        }

        // 2. 자동걷기 기계(일정 속도 무휴식 연속 보행) 판별
        if (stepTimestampsMs.size >= 60) {
            val intervals = mutableListOf<Double>()
            val list = stepTimestampsMs.toList()
            for (i in 1 until list.size) {
                val diff = (list[i] - list[i - 1]).toDouble()
                if (diff in 200.0..2000.0) {
                    intervals.add(diff)
                }
            }
            if (intervals.size >= 40) {
                val mean = intervals.average()
                val variance = intervals.map { (it - mean) * (it - mean) }.average()
                val stdDev = sqrt(variance)
                val cv = if (mean > 0) stdDev / mean else 1.0 // 변동계수 (Coefficient of Variation)

                // 기계적 등속 운동(변동계수 2.5% 미만)이거나 휴식 없이 10,000보 이상 일정 속도로 지속될 때
                if ((cv < 0.025 && gpsMovedMetersLast10Min < 10f) || (continuousUnbrokenSteps >= 10_000 && cv < 0.04)) {
                    return VerificationResult(
                        status = "flagged_machine",
                        reason = "동일 속도의 무휴식 기계적 패턴 감지 (변동률 ${(cv * 100).toInt()}%, 자동걷기 장비 차단)"
                    )
                }
            }
        }

        return VerificationResult(
            status = "verified",
            reason = "정상 어르신 보행 리듬 인증됨 (공정 순위 100% 반영 중)"
        )
    }
}
