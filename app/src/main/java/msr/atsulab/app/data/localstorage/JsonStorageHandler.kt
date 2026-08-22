package msr.atsulab.app.data.localstorage

import msr.atsulab.app.data.response.HomeData
import msr.atsulab.app.data.response.anilist.MediaTag
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.data.response.Genre
import msr.atsulab.app.data.response.anilist.MediaListCollection
import msr.atsulab.app.helper.pojo.SaveItem

interface JsonStorageHandler {
    var homeData: SaveItem<HomeData>?
    var viewerData: SaveItem<User>?
    var genres: SaveItem<List<Genre>>?
    var tags: SaveItem<List<MediaTag>>?
    var animeList: SaveItem<MediaListCollection>?
    var mangaList: SaveItem<MediaListCollection>?
}