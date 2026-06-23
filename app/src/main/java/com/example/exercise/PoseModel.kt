package com.example.exercise

/** Abstract pose input so the verification logic is independent of MediaPipe.
 *  The camera layer maps MediaPipe's 33 landmarks into these. */
data class Landmark(val x: Float, val y: Float, val visibility: Float = 1f)

enum class Joint { SHOULDER, ELBOW, WRIST, HIP, KNEE, ANKLE, HEAD }

/** One frame of landmarks (already side-selected, e.g. left side). */
data class PoseFrame(val points: Map<Joint, Landmark>, val timestampMs: Long)

enum class Exercise { SQUAT, PUSHUP, PULLUP }
enum class MetricType { ANGLE, HEIGHT }

/** {"exercise":"pushup","repCount":1,"confidence":89,"valid":true} */
data class RepResult(val exercise: String, val repCount: Int, val confidence: Int, val valid: Boolean)
