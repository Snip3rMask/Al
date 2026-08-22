package msr.atsulab.app.ui.media.themes

import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.response.AnimeTheme
import msr.atsulab.app.data.response.AnimeThemeEntry
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.databinding.DialogBottomSheetListBinding
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.ui.base.BaseDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class BottomSheetMediaThemesDialog : BaseDialogFragment<DialogBottomSheetListBinding>() {

    private val viewModel by viewModel<BottomSheetMediaThemesViewModel>()

    private lateinit var listener: BottomSheetMediaThemeListener
    private lateinit var media: Media
    private lateinit var animeTheme: AnimeTheme
    private var animeThemeEntry: AnimeThemeEntry? = null

    private var adapter: BottomSheetMediaThemeRvAdapter? = null

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogBottomSheetListBinding {
        return DialogBottomSheetListBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        adapter = BottomSheetMediaThemeRvAdapter(requireContext(), listOf(), object : BottomSheetMediaThemeRvAdapter.BottomSheetMediaThemeListener {
            override fun playWithPlayer(url: String) {
                listener.playWithPlayer(url)
            }

            override fun getYouTubeVideo(searchQuery: String) {
                viewModel.getYouTubeVideo(searchQuery)
            }

            override fun getSpotifyTrack(searchQuery: String) {
                viewModel.getSpotifyTrack(searchQuery)
            }
        })
        binding.dialogRecyclerView.adapter = adapter
    }

    override fun setUpObserver() {
        disposables.addAll(
            viewModel.themeItems.subscribe {
                adapter?.updateData(it)
            },
            viewModel.loading.subscribe {
                binding.loadingLayout.loadingLayout.show(it)
            },
            viewModel.youTubeVideo.subscribe {
                listener.playWithYouTube(it.videoId)
            },
            viewModel.spotifyTrack.subscribe {
                listener.playWithSpotify(it.trackUrl)
            }
        )

        viewModel.loadData(BottomSheetMediaThemesParam(media, animeTheme, animeThemeEntry))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    companion object {
        fun newInstance(
            media: Media,
            animeTheme: AnimeTheme,
            animeThemeEntry: AnimeThemeEntry?,
            listener: BottomSheetMediaThemeListener
        ) = BottomSheetMediaThemesDialog().apply {
            this.media = media
            this.animeTheme = animeTheme
            this.animeThemeEntry = animeThemeEntry
            this.listener = listener
        }
    }

    interface BottomSheetMediaThemeListener {
        fun playWithPlayer(url: String)
        fun playWithYouTube(videoId: String)
        fun playWithSpotify(url: String)
    }
}