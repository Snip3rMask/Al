package msr.atsulab.app.data.datasource

import msr.atsulab.app.data.response.github.AnnouncementResponse
import io.reactivex.rxjava3.core.Observable

interface InfoDataSource {
    fun getAnnouncement(): Observable<AnnouncementResponse>
}