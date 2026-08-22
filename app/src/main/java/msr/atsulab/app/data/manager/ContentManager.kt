package msr.atsulab.app.data.manager

import msr.atsulab.app.data.response.HomeData
import msr.atsulab.app.data.response.anilist.MediaTag
import msr.atsulab.app.data.response.Genre
import msr.atsulab.app.helper.pojo.SaveItem

interface ContentManager {
    var homeData: SaveItem<HomeData>?
    var genres: SaveItem<List<Genre>>?
    var tags: SaveItem<List<MediaTag>>?
}