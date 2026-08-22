package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.type.NotificationType

data class ThreadLikeNotification(
    override val id: Int = 0,
    val userId: Int = 0,
    override val type: NotificationType = NotificationType.THREAD_LIKE,
    val threadId: Int = 0,
    val context: String = "",
    override val createdAt: Int = 0,
    val thread: Thread = Thread(),
    val comment: ThreadComment = ThreadComment(),
    val user: User = User()
) : Notification {
    override fun getMessage(appSetting: AppSetting): String {
        return "${user.name}${context}${thread.title}"
    }
}