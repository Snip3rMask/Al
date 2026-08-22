package msr.atsulab.app.ui.character.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.databinding.LayoutInfiniteScrollingBinding
import msr.atsulab.app.helper.extensions.applyBottomPaddingInsets
import msr.atsulab.app.helper.extensions.applyTopPaddingInsets
import msr.atsulab.app.helper.extensions.getStringResource
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.pojo.CharacterMediaListAdapterComponent
import msr.atsulab.app.helper.utils.GridSpacingItemDecoration
import msr.atsulab.app.ui.base.BaseFragment
import msr.atsulab.app.ui.character.CharacterListener
import msr.atsulab.app.ui.character.CharacterMediaRvAdapter

import org.koin.androidx.viewmodel.ext.android.viewModel

class CharacterMediaListFragment : BaseFragment<LayoutInfiniteScrollingBinding, CharacterMediaListViewModel>() {

    override val viewModel: CharacterMediaListViewModel by viewModel()

    private var adapter: CharacterMediaRvAdapter? = null
    private var adapterComponent = CharacterMediaListAdapterComponent()

    private var menuSortBy: MenuItem? = null
    private var menuMediaType: MenuItem? = null
    private var menuShowHideOnList: MenuItem? = null

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LayoutInfiniteScrollingBinding {
        return LayoutInfiniteScrollingBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            setUpToolbar(defaultToolbar.defaultToolbar, getString(R.string.media_list))
            defaultToolbar.defaultToolbar.inflateMenu(R.menu.menu_character_media_list)
            defaultToolbar.defaultToolbar.subtitle = getString(R.string.sorted_by_x, getString(adapterComponent.mediaSort.getStringResource()))

            menuSortBy = defaultToolbar.defaultToolbar.menu.findItem(R.id.itemSortBy)
            menuMediaType = defaultToolbar.defaultToolbar.menu.findItem(R.id.itemMediaType)
            menuShowHideOnList = defaultToolbar.defaultToolbar.menu.findItem(R.id.itemShowHideOnList)

            menuSortBy?.setOnMenuItemClickListener {
                viewModel.loadMediaSorts()
                true
            }

            menuMediaType?.setOnMenuItemClickListener {
                viewModel.loadMediaTypes()
                true
            }

            menuShowHideOnList?.setOnMenuItemClickListener {
                viewModel.loadShowHideOnLists()
                true
            }

            adapter = CharacterMediaRvAdapter(requireContext(), listOf(), adapterComponent.appSetting, adapterComponent.mediaSort, getCharacterMediaListener())
            infiniteScrollingRecyclerView.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.gridSpan))
            infiniteScrollingRecyclerView.addItemDecoration(GridSpacingItemDecoration(resources.getInteger(R.integer.gridSpan), resources.getDimensionPixelSize(R.dimen.marginNormal), false))
            infiniteScrollingRecyclerView.adapter = adapter

            infiniteScrollingSwipeRefresh.setOnRefreshListener {
                viewModel.reloadData()
            }

            infiniteScrollingRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && !recyclerView.canScrollVertically(1)) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    override fun setUpInsets() {
        binding.defaultToolbar.defaultToolbar.applyTopPaddingInsets()
        binding.infiniteScrollingRecyclerView.applyBottomPaddingInsets()
    }

    override fun setUpObserver() {
        disposables.addAll(
            viewModel.loading.subscribe {
                binding.infiniteScrollingSwipeRefresh.isRefreshing = it
            },
            viewModel.error.subscribe {
                dialog.showToast(it)
            },
            viewModel.adapterComponent.subscribe {
                adapterComponent = it
                adapter = CharacterMediaRvAdapter(requireContext(), listOf(), it.appSetting, it.mediaSort, getCharacterMediaListener())
                binding.infiniteScrollingRecyclerView.adapter = adapter
                binding.defaultToolbar.defaultToolbar.subtitle = getString(R.string.sorted_by_x, getString(adapterComponent.mediaSort.getStringResource()))
            },
            viewModel.media.subscribe {
                adapter?.updateData(it, true)
            },
            viewModel.emptyLayoutVisibility.subscribe {
                binding.emptyLayout.emptyLayout.show(it)
            },
            viewModel.mediaSortList.subscribe {
                dialog.showListDialog(it) { data, _ ->
                    viewModel.updateMediaSort(data)
                }
            },
            viewModel.mediaTypeList.subscribe {
                dialog.showListDialog(it) { data, _ ->
                    viewModel.updateMediaType(data)
                }
            },
            viewModel.showHideOnListList.subscribe {
                dialog.showListDialog(it) { data, _ ->
                    viewModel.updateShowHideOnList(data)
                }
            }
        )

        arguments?.getInt(CHARACTER_ID)?.let {
            viewModel.loadData(CharacterMediaListParam(it))
        }
    }

    private fun getCharacterMediaListener(): CharacterListener.CharacterMediaListener {
        return object : CharacterListener.CharacterMediaListener {
            override fun navigateToMedia(media: Media) {
                navigation.navigateToMedia(media.getId())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        menuSortBy = null
        menuMediaType = null
        menuShowHideOnList = null
    }

    companion object {
        private const val CHARACTER_ID = "characterId"
        @JvmStatic
        fun newInstance(characterId: Int) =
            CharacterMediaListFragment().apply {
                arguments = Bundle().apply {
                    putInt(CHARACTER_ID, characterId)
                }
            }
    }
}