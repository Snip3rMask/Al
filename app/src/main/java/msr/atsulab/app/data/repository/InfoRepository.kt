package msr.atsulab.app.data.repository

import msr.atsulab.app.data.response.Announcement
import io.reactivex.rxjava3.core.Observable

interface InfoRepository {
    fun getAnnouncement(): Observable<Announcement>
    fun getLastAnnouncementId(): Observable<String>
    fun setLastAnnouncementId(announcementId: String)
}