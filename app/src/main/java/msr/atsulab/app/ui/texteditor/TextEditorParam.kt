package msr.atsulab.app.ui.texteditor

import msr.atsulab.app.helper.enums.TextEditorType

data class TextEditorParam(
    val textEditorType: TextEditorType,
    val activityId: Int? = null,
    val activityReplyId: Int? = null,
    val recipientId: Int? = null,
    val username: String? = null
)
