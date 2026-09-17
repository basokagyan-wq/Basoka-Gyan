package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.ScreenHierarchyData
import com.example.model.ScreenNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BasitAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event tracking
    }

    override fun onInterrupt() {
        // Handled
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
    }

    fun executeBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun executeHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun executeRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun executeNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun executeQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun readCurrentScreen(): ScreenHierarchyData {
        val root = rootInActiveWindow ?: return ScreenHierarchyData(title = "شاشە لە دەستدا نییە")
        val texts = mutableListOf<String>()
        val buttons = mutableListOf<String>()
        val editables = mutableListOf<String>()
        val allNodes = mutableListOf<ScreenNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val nodeText = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val label = when {
                nodeText.isNotBlank() -> nodeText
                desc.isNotBlank() -> desc
                else -> ""
            }

            if (label.isNotBlank()) {
                texts.add(label)
                if (node.isClickable) {
                    buttons.add(label)
                }
            }

            if (node.isEditable) {
                editables.add(label.ifBlank { "خانەی نووسین" })
            }

            if (label.isNotBlank() || node.isClickable || node.isEditable) {
                allNodes.add(
                    ScreenNodeInfo(
                        text = label,
                        className = node.className?.toString() ?: "",
                        isClickable = node.isClickable,
                        isEditable = node.isEditable,
                        viewId = node.viewIdResourceName
                    )
                )
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)
        val screenTitle = texts.firstOrNull() ?: "شاشەی ئێستا"
        return ScreenHierarchyData(
            title = screenTitle,
            visibleTexts = texts.distinct(),
            clickableButtons = buttons.distinct(),
            editableFields = editables.distinct(),
            allNodes = allNodes
        )
    }

    fun clickElementByText(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanQuery = query.trim().lowercase()

        fun findAndClick(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            if (text.contains(cleanQuery) || desc.contains(cleanQuery)) {
                var clickTarget: AccessibilityNodeInfo? = node
                while (clickTarget != null && !clickTarget.isClickable) {
                    clickTarget = clickTarget.parent
                }
                if (clickTarget != null && clickTarget.isClickable) {
                    return clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }

            for (i in 0 until node.childCount) {
                if (findAndClick(node.getChild(i))) return true
            }
            return false
        }

        return findAndClick(root)
    }

    fun scrollScreen(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findAndScroll(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isScrollable) {
                val action = if (forward) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                return node.performAction(action)
            }
            for (i in 0 until node.childCount) {
                if (findAndScroll(node.getChild(i))) return true
            }
            return false
        }

        return findAndScroll(root)
    }

    fun typeIntoField(inputText: String): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findFocusedAndType(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isEditable && (node.isFocused || node.isAccessibilityFocused)) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, inputText)
                }
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
            for (i in 0 until node.childCount) {
                if (findFocusedAndType(node.getChild(i))) return true
            }
            return false
        }

        return findFocusedAndType(root)
    }

    companion object {
        private var instance: BasitAccessibilityService? = null
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun get(): BasitAccessibilityService? = instance
        fun isEnabled(): Boolean = instance != null
    }
}
