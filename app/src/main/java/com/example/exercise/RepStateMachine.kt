package com.example.exercise

/**
 * Counts reps only on a FULL range of motion: TOP -> BOTTOM (past threshold) -> TOP.
 * Half-reps are rejected. Emits a confidence score per accepted rep.
 */
class RepStateMachine(
    private val spec: ExerciseSpec,
    private val confidenceThreshold: Int = 80,
    private val onRep: (repIndex: Int, confidence: Int) -> Unit,
    private val onReject: (reason: String) -> Unit = {}
) {
    enum class State { TOP, DESCENDING, BOTTOM, ASCENDING }

    private var state = State.TOP
    private var reps = 0
    private var minReached = Double.MAX_VALUE   // deepest flexion this rep (ANGLE: smallest angle)
    private var maxReached = -Double.MAX_VALUE
    private val samples = mutableListOf<Double>()

    /** Feed the current rep metric (joint angle in degrees, or head-height delta). */
    fun update(metric: Double) {
        samples.add(metric)
        when (spec.metric) {
            MetricType.ANGLE -> angleUpdate(metric)
            MetricType.HEIGHT -> heightUpdate(metric)
        }
    }

    private fun angleUpdate(angle: Double) {
        minReached = minOf(minReached, angle)
        maxReached = maxOf(maxReached, angle)
        when (state) {
            State.TOP -> if (angle < spec.topAngle - 15) { state = State.DESCENDING; minReached = angle }
            State.DESCENDING -> {
                if (angle <= spec.bottomAngle) state = State.BOTTOM
                else if (angle >= spec.topAngle - 5) { onReject("did not reach depth"); resetRep() } // bounced back early
            }
            State.BOTTOM -> if (angle > spec.bottomAngle + 10) state = State.ASCENDING
            State.ASCENDING -> if (angle >= spec.topAngle - 8) finishRep()
        }
    }

    private fun heightUpdate(delta: Double) {
        // delta = head above wrist (normalized). Higher = chin above bar.
        maxReached = maxOf(maxReached, delta)
        when (state) {
            State.TOP -> if (delta > spec.topAngle) state = State.ASCENDING
            State.ASCENDING -> if (delta >= spec.bottomAngle) state = State.BOTTOM
            State.BOTTOM -> if (delta < spec.topAngle) finishRep()
            else -> {}
        }
    }

    private fun finishRep() {
        val conf = ConfidenceScorer.score(spec, minReached, maxReached, samples)
        if (conf >= confidenceThreshold) { reps++; onRep(reps, conf) }
        else onReject("low confidence ($conf)")
        resetRep()
    }

    private fun resetRep() {
        state = State.TOP; minReached = Double.MAX_VALUE; maxReached = -Double.MAX_VALUE; samples.clear()
    }

    fun repCount() = reps
}
