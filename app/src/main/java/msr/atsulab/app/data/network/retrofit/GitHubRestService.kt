package msr.atsulab.app.data.network.retrofit

import msr.atsulab.app.data.response.github.AnnouncementResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET

interface GitHubRestService {

    @GET("docs/json/announcement.json")
    fun getAnnouncement(): Observable<AnnouncementResponse>
}