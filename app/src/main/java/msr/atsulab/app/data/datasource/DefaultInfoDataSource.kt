package msr.atsulab.app.data.datasource

import msr.atsulab.app.data.network.retrofit.RetrofitHandler
import msr.atsulab.app.data.response.github.AnnouncementResponse
import io.reactivex.rxjava3.core.Observable

class DefaultInfoDataSource(private val retrofitHandler: RetrofitHandler) : InfoDataSource {

    override fun getAnnouncement(): Observable<AnnouncementResponse> {
        return retrofitHandler.gitHubRetrofitClient().getAnnouncement()
    }
}