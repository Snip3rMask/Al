package msr.atsulab.app.data.network.interceptor

import msr.atsulab.app.data.manager.BrowseManager
import okhttp3.Interceptor
import okhttp3.Response

class SpotifyAuthHeaderInterceptor(private val browseManager: BrowseManager) : HeaderInterceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request().newBuilder()
                .addHeader("Authorization", browseManager.spotifyApiKey)
                .build()
        )
    }
}