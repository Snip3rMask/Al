package msr.atsulab.app

import android.app.Application
import com.google.gson.GsonBuilder
import msr.atsulab.app.data.datasource.*
import msr.atsulab.app.data.localstorage.*
import msr.atsulab.app.data.manager.*
import msr.atsulab.app.data.network.apollo.AniListApolloHandler
import msr.atsulab.app.data.network.apollo.ApolloHandler
import msr.atsulab.app.data.network.interceptor.AniListHeaderInterceptor
import msr.atsulab.app.data.network.interceptor.HeaderInterceptor
import msr.atsulab.app.data.network.interceptor.SpotifyAuthHeaderInterceptor
import msr.atsulab.app.data.network.interceptor.SpotifyHeaderInterceptor
import msr.atsulab.app.data.network.retrofit.DefaultRetrofitHandler
import msr.atsulab.app.data.network.retrofit.RetrofitHandler
import msr.atsulab.app.data.repository.*
import msr.atsulab.app.helper.Constant
import msr.atsulab.app.helper.crash.CrashReporter
import msr.atsulab.app.helper.service.clipboard.ClipboardService
import msr.atsulab.app.helper.service.clipboard.DefaultClipboardService
import msr.atsulab.app.helper.service.pushnotification.DefaultPushNotificationService
import msr.atsulab.app.helper.service.pushnotification.PushNotificationService
import msr.atsulab.app.player.di.playbackDownloadModule
import msr.atsulab.app.player.di.playbackNetworkModule
import msr.atsulab.app.player.di.playbackEngineModule
import msr.atsulab.app.player.di.playbackProviderModule
import msr.atsulab.app.player.di.playbackRepositoryModule
import msr.atsulab.app.player.di.playbackStorageModule
import msr.atsulab.app.ui.activity.ActivityDetailViewModel
import msr.atsulab.app.ui.activity.ActivityListViewModel
import msr.atsulab.app.ui.base.BaseActivityViewModel
import msr.atsulab.app.ui.calendar.CalendarViewModel
import msr.atsulab.app.ui.character.CharacterViewModel
import msr.atsulab.app.ui.character.media.CharacterMediaListViewModel
import msr.atsulab.app.ui.common.BottomSheetMediaQuickDetailViewModel
import msr.atsulab.app.ui.customise.CustomiseViewModel
import msr.atsulab.app.ui.editor.EditorViewModel
import msr.atsulab.app.ui.explore.ExploreViewModel
import msr.atsulab.app.ui.favorite.FavoriteViewModel
import msr.atsulab.app.ui.filter.FilterViewModel
import msr.atsulab.app.ui.follow.FollowViewModel
import msr.atsulab.app.ui.home.HomeViewModel
import msr.atsulab.app.ui.landing.LandingViewModel
import msr.atsulab.app.ui.login.LoginViewModel
import msr.atsulab.app.ui.main.MainViewModel
import msr.atsulab.app.ui.main.SharedMainViewModel
import msr.atsulab.app.ui.media.character.MediaCharacterListViewModel
import msr.atsulab.app.ui.media.MediaViewModel
import msr.atsulab.app.ui.media.mediasocial.MediaSocialViewModel
import msr.atsulab.app.ui.media.mediastats.MediaStatsViewModel
import msr.atsulab.app.ui.media.staff.MediaStaffListViewModel
import msr.atsulab.app.ui.media.themes.BottomSheetMediaThemesViewModel
import msr.atsulab.app.ui.medialist.BottomSheetMediaListQuickDetailViewModel
import msr.atsulab.app.ui.medialist.MediaListViewModel
import msr.atsulab.app.ui.notifications.NotificationsViewModel
import msr.atsulab.app.ui.profile.ProfileViewModel
import msr.atsulab.app.ui.reorder.ReorderViewModel
import msr.atsulab.app.ui.review.ReviewViewModel
import msr.atsulab.app.ui.review.reader.ReaderViewModel
import msr.atsulab.app.ui.search.SearchViewModel
import msr.atsulab.app.ui.seasonal.SeasonalViewModel
import msr.atsulab.app.ui.settings.SettingsViewModel
import msr.atsulab.app.ui.settings.account.AccountSettingsViewModel
import msr.atsulab.app.ui.settings.anilist.AniListSettingsViewModel
import msr.atsulab.app.ui.settings.app.AppSettingsViewModel
import msr.atsulab.app.ui.settings.capture.CaptureSettingsViewModel
import msr.atsulab.app.ui.settings.list.ListSettingsViewModel
import msr.atsulab.app.ui.settings.notifications.NotificationsSettingsViewModel
import msr.atsulab.app.ui.social.SocialViewModel
import msr.atsulab.app.ui.splash.SplashViewModel
import msr.atsulab.app.ui.staff.StaffViewModel
import msr.atsulab.app.ui.staff.character.StaffCharacterListViewModel
import msr.atsulab.app.ui.staff.media.StaffMediaListViewModel
import msr.atsulab.app.ui.studio.StudioViewModel
import msr.atsulab.app.ui.studio.media.StudioMediaListViewModel
import msr.atsulab.app.ui.texteditor.TextEditorViewModel
import msr.atsulab.app.ui.userstats.UserStatsViewModel

import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ALchanApplication : Application() {

    private val appModules = module {
        val gson = GsonBuilder()
            .setLenient()
            .serializeSpecialFloatingPointValues()
            .create()

        // local storage
        single<SharedPreferencesHandler> {
            DefaultSharedPreferencesHandler(
                this@ALchanApplication.applicationContext,
                Constant.SHARED_PREFERENCES_NAME,
                gson
            )
        }

        single<JsonStorageHandler> {
            DefaultJsonStorageHandler(
                this@ALchanApplication,
                gson
            )
        }

        single<FileStorageHandler> {
            DefaultFileStorageHandler(this@ALchanApplication)
        }

        // local storage manager
        single<UserManager> { DefaultUserManager(get(), get(), get()) }
        single<ContentManager> { DefaultContentManager(get()) }
        single<BrowseManager> { DefaultBrowseManager(get()) }

        // network
        val aniListHeaderInterceptor = "aniListHeaderInterceptor"
        val spotifyAuthHeaderInterceptor = "spotifyAuthHeaderInterceptor"
        val spotifyHeaderInterceptor = "spotifyHeaderInterceptor"

        single<HeaderInterceptor>(named(aniListHeaderInterceptor)) { AniListHeaderInterceptor(get()) }
        single<HeaderInterceptor>(named(spotifyAuthHeaderInterceptor)) { SpotifyAuthHeaderInterceptor(get()) }
        single<HeaderInterceptor>(named(spotifyHeaderInterceptor)) { SpotifyHeaderInterceptor(get()) }
        single<ApolloHandler> { AniListApolloHandler(get(named(aniListHeaderInterceptor)), Constant.ANILIST_API_BASE_URL) }
        single<RetrofitHandler> {
            DefaultRetrofitHandler(
                Constant.ALCHAN_RAW_GITHUB_URL,
                Constant.JIKAN_API_URL,
                Constant.ANIME_THEMES_API_URL,
                Constant.YOUTUBE_SEARCH_API_URL,
                Constant.SPOTIFY_AUTH_API_URL,
                get(named(spotifyAuthHeaderInterceptor)),
                Constant.SPOTIFY_API_URL,
                get(named(spotifyHeaderInterceptor))
            )
        }

        // data source
        single<ContentDataSource> { DefaultContentDataSource(get(), Constant.ANILIST_API_STATUS_VERSION, Constant.ANILIST_API_SOURCE_VERSION) }
        single<UserDataSource> { DefaultUserDataSource(get()) }
        single<MediaListDataSource> { DefaultMediaListDataSource(get(), Constant.ANILIST_API_STATUS_VERSION, Constant.ANILIST_API_SOURCE_VERSION) }
        single<BrowseDataSource> { DefaultBrowseDataSource(get(), get(), Constant.ANILIST_API_STATUS_VERSION, Constant.ANILIST_API_SOURCE_VERSION, Constant.ANILIST_API_RELATION_TYPE_VERSION) }
        single<SocialDataSource> { DefaultSocialDataSource(get()) }
        single<InfoDataSource> { DefaultInfoDataSource(get()) }

        // repository
        single<ContentRepository> { DefaultContentRepository(get(), get()) }
        single<UserRepository> { DefaultUserRepository(get(), get()) }
        single<MediaListRepository> { DefaultMediaListRepository(get(), get()) }
        single<BrowseRepository> { DefaultBrowseRepository(get(), get()) }
        single<SocialRepository> { DefaultSocialRepository(get()) }
        single<InfoRepository> { DefaultInfoRepository(get(), get()) }

        // service
        single<ClipboardService> { DefaultClipboardService(this.androidContext()) }
        single<PushNotificationService> { DefaultPushNotificationService(this.androidContext(), get()) }

        // view model
        viewModel { BaseActivityViewModel(get()) }

        viewModel { SplashViewModel(get(), get()) }
        viewModel { LandingViewModel() }
        viewModel { LoginViewModel(get()) }

        viewModel { SharedMainViewModel() }
        viewModel { MainViewModel(get(), get(), get()) }

        viewModel { BottomSheetMediaQuickDetailViewModel(get()) }
        viewModel { BottomSheetMediaListQuickDetailViewModel(get(), get()) }
        viewModel { BottomSheetMediaThemesViewModel(get()) }

        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel { SearchViewModel(get(), get()) }
        viewModel { SeasonalViewModel(get(), get(), get()) }
        viewModel { ExploreViewModel(get(), get()) }
        viewModel { CalendarViewModel(get(), get()) }
        viewModel { ReviewViewModel(get(), get()) }
        viewModel { ReaderViewModel(get(), get(), get()) }

        viewModel { MediaListViewModel(get(), get(), get(), get()) }

        viewModel { NotificationsViewModel(get()) }

        viewModel { ProfileViewModel(get(), get(), get(), get()) }
        viewModel { FollowViewModel(get()) }
        viewModel { UserStatsViewModel(get(), get()) }
        viewModel { FavoriteViewModel(get()) }

        viewModel { SettingsViewModel() }
        viewModel { AppSettingsViewModel(get(), get()) }
        viewModel { CaptureSettingsViewModel(get()) }
        viewModel { AniListSettingsViewModel(get()) }
        viewModel { ListSettingsViewModel(get()) }
        viewModel { NotificationsSettingsViewModel(get()) }
        viewModel { AccountSettingsViewModel(get()) }

        viewModel { ReorderViewModel() }

        viewModel { FilterViewModel(get(), get()) }
        viewModel { CustomiseViewModel(get(), get()) }

        viewModel { EditorViewModel(get(), get()) }

        viewModel { MediaViewModel(get(), get(), get(), get()) }
        viewModel { MediaStatsViewModel(get()) }
        viewModel { MediaSocialViewModel(get(), get()) }
        viewModel { MediaCharacterListViewModel(get(), get()) }
        viewModel { MediaStaffListViewModel(get(), get()) }
        viewModel { CharacterViewModel(get(), get(), get()) }
        viewModel { CharacterMediaListViewModel(get(), get()) }
        viewModel { StaffViewModel(get(), get(), get()) }
        viewModel { StaffCharacterListViewModel(get(), get()) }
        viewModel { StaffMediaListViewModel(get(), get()) }
        viewModel { StudioViewModel(get(), get(), get()) }
        viewModel { StudioMediaListViewModel(get(), get()) }

        viewModel { SocialViewModel(get(), get(), get()) }
        viewModel { ActivityDetailViewModel(get(), get(), get()) }
        viewModel { ActivityListViewModel(get(), get(), get()) }
        viewModel { TextEditorViewModel(get(), get()) }
    }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ALchanApplication)
            modules(
                appModules,
                playbackEngineModule,
                playbackDownloadModule,
                playbackNetworkModule,
                playbackProviderModule,
                playbackStorageModule,
                playbackRepositoryModule
            )
        }
    }
}
