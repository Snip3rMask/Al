package msr.atsulab.app.data.converter

import msr.atsulab.app.FollowersQuery
import msr.atsulab.app.data.response.anilist.Page
import msr.atsulab.app.data.response.anilist.PageInfo
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.data.response.anilist.UserAvatar

fun FollowersQuery.Data.convert(): Page<User> {
    return Page(
        PageInfo(
            total = Page?.pageInfo?.total ?: 0,
            perPage = Page?.pageInfo?.perPage ?: 0,
            currentPage = Page?.pageInfo?.currentPage ?: 0,
            lastPage = Page?.pageInfo?.lastPage ?: 0,
            hasNextPage = Page?.pageInfo?.hasNextPage ?: false
        ),
        Page?.followers?.filterNotNull()?.map {
            User(
                id = it.id,
                name = it.name,
                avatar = UserAvatar(
                    large = it.avatar?.large ?: "",
                    medium = it.avatar?.medium ?: ""
                ),
                bannerImage = it.bannerImage ?: "",
                isFollowing = it.isFollowing ?: false,
                isFollower = it.isFollower ?: false,
                siteUrl = it.siteUrl ?: ""
            )
        } ?: listOf()
    )
}