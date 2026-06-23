package com.example.exercise

/**
 * Verification seam. MVP can use the accelerometer (com.example.tracking.RepCounter);
 * V2 plugs in a MediaPipe pose source that feeds PoseFrames into a RepStateMachine.
 */
interface RepVerifier {
    fun start()
    fun stop()
    fun reps(): Int
}

/**
 * Pose-based verifier: drive it by calling onFrame() from the MediaPipe PoseLandmarker
 * result listener. Selects the right joints per exercise, computes the metric, and
 * delegates rep counting + cheat rejection to RepStateMachine.
 */
class PoseRepVerifier(
    private val spec: ExerciseSpec,
    private val target: Int,
    private val onRep: (Int, Int) -> Unit,
    private val onDone: () -> Unit,
    private val onReject: (String) -> Unit = {}
) : RepVerifier {
    private var running = false
    private var lastConfidence = 0
    private val sm = RepStateMachine(spec, onRep = { i, c ->
        lastConfidence = c; onRep(i, c); if (i >= target) { running = false; onDone() }
    }, onReject = onReject)

    override fun start() { running = true }
    override fun stop() { running = false }
    override fun reps() = sm.repCount()

    /** Structured result matching processCameraFrame() output. */
    fun result() = RepResult(
        exercise = spec.exercise.name.lowercase(),
        repCount = sm.repCount(),
        confidence = lastConfidence,
        valid = sm.repCount() > 0
    )

    /** Call once per MediaPipe frame. */
    fun onFrame(frame: PoseFrame) {
        if (!running) return
        val p = frame.points
        val metric = when (spec.exercise) {
            Exercise.SQUAT -> PoseMath.angle(p[Joint.HIP] ?: return, p[Joint.KNEE] ?: return, p[Joint.ANKLE] ?: return)
            Exercise.PUSHUP -> PoseMath.angle(p[Joint.SHOULDER] ?: return, p[Joint.ELBOW] ?: return, p[Joint.WRIST] ?: return)
            Exercise.PULLUP -> PoseMath.heightAbove(p[Joint.HEAD] ?: return, p[Joint.WRIST] ?: return)
        }
        sm.update(metric)
    }
}
