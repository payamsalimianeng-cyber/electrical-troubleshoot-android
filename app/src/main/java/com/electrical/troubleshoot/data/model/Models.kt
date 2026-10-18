package com.electrical.troubleshoot.data.model

data class User(
    val id: String,
    val name: String,
    val username: String,
    val role: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: User?,
    val message: String? = null
)

data class Room(
    val _id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val order: Int = 0,
    val mechanisms: List<Mechanism>? = null
)

data class Mechanism(
    val _id: String,
    val room: String? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val pdfFile: String? = null,
    val order: Int = 0,
    val points: List<AnnotatedPoint>? = null
)

data class AnnotatedPoint(
    val _id: String,
    val mechanism: String? = null,
    val name: String,
    val type: String, // RELAY, TERMINAL, WIRE, FUSE, ...
    val page: Int = 1,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val instruction: String? = null,
    val priority: Int = 50,
    val tags: List<String>? = null
)

data class ChecklistStep(
    val _id: String,
    val point: AnnotatedPoint,
    val order: Int,
    val status: String, // PENDING, CHECKED, NOT_FOUND, SKIPPED
    val note: String? = null,
    val checkedAt: String? = null
)

data class Session(
    val _id: String,
    val room: Room? = null,
    val mechanism: Mechanism? = null,
    val problemDescription: String,
    val status: String,
    val steps: List<ChecklistStep>,
    val resolutionNote: String? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null
)

data class Experience(
    val _id: String,
    val title: String,
    val summary: String,
    val problem: String,
    val solution: String,
    val tags: List<String>? = null,
    val room: Room? = null,
    val mechanism: Mechanism? = null,
    val usefulCount: Int = 0
)

data class Sop(
    val _id: String,
    val title: String,
    val code: String? = null,
    val content: String,
    val version: Int = 1,
    val tags: List<String>? = null
)

// API wrappers
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val count: Int? = null,
    val message: String? = null
)

data class StartSessionRequest(
    val roomId: String,
    val mechanismId: String,
    val problemDescription: String
)

data class UpdateStepRequest(
    val status: String,
    val note: String? = null
)
