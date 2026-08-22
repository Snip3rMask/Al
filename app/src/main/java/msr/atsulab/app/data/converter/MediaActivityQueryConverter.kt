package msr.atsulab.app.data.converter

import msr.atsulab.app.MediaActivityQuery
import msr.atsulab.app.data.response.anilist.Activity
import msr.atsulab.app.data.response.anilist.ListActivity
import msr.atsulab.app.data.response.anilist.Page
import msr.atsulab.app.data.response.anilist.PageInfo

fun MediaActivityQuery.Data.convert(): Page<ListActivity> {
    return Page(
        pageInfo = PageInfo(
            total = Page?.pageInfo?.total ?: 0,
            perPage = Page?.pageInfo?.perPage ?: 0,
            currentPage = Page?.pageInfo?.currentPage ?: 0,
            lastPage = Page?.pageInfo?.lastPage ?: 0,
            hasNextPage = Page?.pageInfo?.hasNextPage ?: false
        ),
        data = Page?.activities?.filterNotNull()?.map { activity ->
            activity.onListActivity?.convert() ?: ListActivity()
        } ?: listOf()
    )
}