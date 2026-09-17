package com.example.model

enum class ActionCategory {
    SYSTEM,
    MEDIA,
    COMMUNICATION,
    ACCESSIBILITY,
    ALARM_REMINDER,
    AI_VISION,
    SETTINGS,
    RESEARCH
}

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
    PENDING_CONFIRMATION,
    CANCELLED
}

data class GroundingSource(
    val title: String,
    val uri: String
)

data class SocialAccount(
    val platform: String,
    val handleOrUrl: String
)

data class RecognizedPerson(
    val fullName: String,
    val profession: String = "",
    val biography: String = "",
    val accounts: List<SocialAccount> = emptyList()
)

data class PlaceLocation(
    val placeName: String,
    val cityAndCountry: String = "",
    val coordinates: String = "",
    val address: String = "",
    val mapsQuery: String = "",
    val description: String = ""
)

data class ParsedCommand(
    val intent: String,
    val target: String = "",
    val parameter: String = "",
    val requiresConfirmation: Boolean = false,
    val kurdishSpeech: String = "",
    val kurdishSummary: String = "",
    val category: ActionCategory = ActionCategory.SYSTEM,
    val sources: List<GroundingSource> = emptyList()
)

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val command: ParsedCommand? = null,
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val resultMessage: String? = null,
    val attachmentUri: String? = null,
    val attachmentType: String? = null,
    val sources: List<GroundingSource> = emptyList(),
    val isDeepVerified: Boolean = false,
    val recognizedPerson: RecognizedPerson? = null,
    val placeLocation: PlaceLocation? = null,
    val imageBitmap: android.graphics.Bitmap? = null
)

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    AWAITING_CONFIRMATION
}

data class ScreenNodeInfo(
    val text: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val viewId: String? = null
)

data class ScreenHierarchyData(
    val title: String = "",
    val visibleTexts: List<String> = emptyList(),
    val clickableButtons: List<String> = emptyList(),
    val editableFields: List<String> = emptyList(),
    val allNodes: List<ScreenNodeInfo> = emptyList()
)

data class PermissionItem(
    val id: String,
    val nameKurdish: String,
    val descriptionKurdish: String,
    val iconName: String,
    val isGranted: Boolean,
    val isSpecialSettings: Boolean = false,
    val androidPermission: String? = null
)
