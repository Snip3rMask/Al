package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.MediaType
import msr.atsulab.app.type.ReviewRating


data class Review(
    val id: Int = 0,
    val userId: Int = 0,
    val mediaId: Int = 0,
    val mediaType: MediaType? = null,
    val summary: String = "",
    val body: String = "",
    var rating: Int = 0,
    var ratingAmount: Int = 0,
    var userRating: ReviewRating? = null,
    val score: Int = 0,
    val private: Boolean = false,
    val siteUrl: String = "",
    val createdAt: Int = 0,
    val updatedAt: Int = 0,
    val user: User = User(),
    val media: Media = Media()
)