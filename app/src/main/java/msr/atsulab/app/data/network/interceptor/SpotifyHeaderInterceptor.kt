package msr.atsulab.app.data.network.interceptor

import android.util.Log
import msr.atsulab.app.data.manager.BrowseManager
import okhttp3.Interceptor
import okhttp3.Response

class SpotifyHeaderInterceptor(private val browseManager: BrowseManager) : HeaderInterceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request().newBuilder()
                .addHeader("Authorization", "Bearer ${browseManager.spotifyAccessToken.accessToken}")
                .build()
        )
    }
}