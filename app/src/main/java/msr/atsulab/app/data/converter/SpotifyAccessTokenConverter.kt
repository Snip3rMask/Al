package msr.atsulab.app.data.converter

import msr.atsulab.app.data.response.SpotifyAccessToken
import msr.atsulab.app.data.response.spotify.SpotifyAccessTokenResponse

fun SpotifyAccessTokenResponse.convert(): SpotifyAccessToken {
    return SpotifyAccessToken(
        accessToken = accessToken ?: "",
        expiresIn = expiresIn ?: 0
    )
}