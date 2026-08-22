package msr.atsulab.app.data.converter

import msr.atsulab.app.RateReviewMutation
import msr.atsulab.app.data.response.anilist.Review

fun RateReviewMutation.Data.convert(): Review {
    return Review(
        id = RateReview?.id ?: 0,
        rating = RateReview?.rating ?: 0,
        ratingAmount = RateReview?.ratingAmount ?: 0,
        userRating = RateReview?.userRating
    )
}