package msr.atsulab.app.data.repository

import msr.atsulab.app.data.converter.convert
import msr.atsulab.app.data.datasource.InfoDataSource
import msr.atsulab.app.data.manager.UserManager
import msr.atsulab.app.data.response.Announcement
import io.reactivex.rxjava3.core.Observable

class DefaultInfoRepository(private val infoDataSource: InfoDataSource, private val userManager: UserManager) : InfoRepository {

    override fun getAnnouncement(): Observable<Announcement> {
        return infoDataSource.getAnnouncement().map {
            it.convert()
        }
    }

    override fun getLastAnnouncementId(): Observable<String> {
        return Observable.just(userManager.lastAnnouncementId ?: "")
    }

    override fun setLastAnnouncementId(announcementId: String) {
        userManager.lastAnnouncementId = announcementId
    }
}