package msr.atsulab.app.data.manager

import msr.atsulab.app.data.localstorage.JsonStorageHandler
import msr.atsulab.app.data.response.HomeData
import msr.atsulab.app.data.response.anilist.MediaTag
import msr.atsulab.app.data.response.Genre
import msr.atsulab.app.helper.pojo.SaveItem

class DefaultContentManager(private val jsonStorageHandler: JsonStorageHandler) : ContentManager {

    override var homeData: SaveItem<HomeData>?
        get() = jsonStorageHandler.homeData
        set(value) { jsonStorageHandler.homeData = value }

    override var genres: SaveItem<List<Genre>>?
        get() = jsonStorageHandler.genres
        set(value) { jsonStorageHandler.genres = value }

    override var tags: SaveItem<List<MediaTag>>?
        get() = jsonStorageHandler.tags
        set(value) { jsonStorageHandler.tags = value }
}