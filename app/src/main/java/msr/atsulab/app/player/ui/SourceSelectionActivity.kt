package msr.atsulab.app.player.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.SourceCandidateSection
import msr.atsulab.app.player.domain.repository.SourceCandidateRepository
import msr.atsulab.app.player.storage.SourceMapping
import msr.atsulab.app.player.storage.SourceMappingStore
import msr.atsulab.app.player.storage.SourcePick
import org.koin.java.KoinJavaComponent.inject

class SourceSelectionActivity : AppCompatActivity() {

    private val sourceCandidateRepository: SourceCandidateRepository by inject(SourceCandidateRepository::class.java)
    private val sourceMappingStore: SourceMappingStore by inject(SourceMappingStore::class.java)

    private lateinit var content: LinearLayout
    private var disposable: Disposable? = null
    private var title: String = ""
    private var aniListId: Int? = null
    private var singleServerType: String? = null

    private var currentSections: List<SourceCandidateSection> = emptyList()
    private val selectedCandidates = LinkedHashMap<String, SourceCandidate>()
    private val skippedProviders = LinkedHashSet<String>()
    private val savedPicks = LinkedHashMap<String, SourcePick>()
    private var hasExistingMapping = false
    private var doneButton: TextView? = null
    private var resetButton: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, INVALID_ANILIST_ID)
            .takeIf { it != INVALID_ANILIST_ID }
        singleServerType = intent.getStringExtra(EXTRA_SINGLE_SERVER_TYPE)?.takeIf(String::isNotEmpty)

        if (title.isBlank() || aniListId == null) {
            Toast.makeText(this, R.string.source_selection_missing_title, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSavedMapping()
        setContentView(buildView())
        window.statusBarColor = PLAYER_BACKGROUND_COLOR
        window.navigationBarColor = PLAYER_BACKGROUND_COLOR
        loadCandidates()
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    private fun loadSavedMapping() {
        val mapping = sourceMappingStore.get(aniListId.toString())
        hasExistingMapping = mapping != null && mapping.picks.isNotEmpty()
        savedPicks.clear()
        savedPicks.putAll(mapping?.picks.orEmpty())
        skippedProviders.clear()
        skippedProviders.addAll(mapping?.skipped.orEmpty())
    }

    private fun buildView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PLAYER_BACKGROUND_COLOR)
        }
        root.addView(buildTopBar(), LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))

        if (singleServerType == null) {
            val instruction = TextView(this).apply {
                text = getString(R.string.source_selection_instruction)
                textSize = 14f
                setTextColor(MUTED_COLOR)
                gravity = Gravity.CENTER
                setPadding(dp(32), 0, dp(32), dp(12))
            }
            root.addView(instruction, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
        }

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setFillViewport(true)
        }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, 0, 1f))

        if (singleServerType == null) {
            root.addView(buildBottomBar(), LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
        }
        return root
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(16), dp(20), dp(16))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            contentDescription = getString(R.string.navigate_back)
            setOnClickListener { finish() }
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(44), dp(44)))

        val label = TextView(this).apply {
            text = getString(
                if (singleServerType == null) R.string.source_selection_title else R.string.source_selection_server_title
            )
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        bar.addView(label, LinearLayout.LayoutParams(0, LINEAR_WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })

        if (singleServerType == null && hasExistingMapping) {
            resetButton = createBarButton(getString(R.string.source_selection_reset)) { confirmReset() }
            bar.addView(resetButton, LinearLayout.LayoutParams(LINEAR_WRAP_CONTENT, LINEAR_WRAP_CONTENT))
        }
        return bar
    }

    private fun buildBottomBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(12), dp(32), dp(20))
        }
        doneButton = TextView(this).apply {
            text = getString(R.string.source_selection_done)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(15))
            setOnClickListener { saveAndFinishIfComplete(showToastWhenIncomplete = true) }
        }
        bar.addView(doneButton, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
        updateDoneButton()
        return bar
    }

    private fun loadCandidates() {
        disposable?.dispose()
        renderLoading()
        disposable = sourceCandidateRepository.findCandidates(title, aniListId, singleServerType)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                ::renderSuccess,
                { renderError() }
            )
    }

    private fun renderLoading() {
        content.removeAllViews()
        repeat(3) {
            content.addView(
                buildSkeletonSection(),
                LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT).apply { bottomMargin = dp(24) }
            )
        }
    }

    private fun renderSuccess(loadedSections: List<SourceCandidateSection>) {
        currentSections = loadedSections
        restoreSavedSelections()
        renderSections(preserveScroll = false)
    }

    private fun restoreSavedSelections() {
        selectedCandidates.clear()
        val validLabels = currentSections.mapTo(mutableSetOf(), SourceCandidateSection::displayName)
        skippedProviders.retainAll(validLabels)
        currentSections.forEach { section ->
            if (section.displayName in skippedProviders) return@forEach
            savedPicks[section.displayName]?.takeIf { it.id.isNotBlank() }?.let { pick ->
                section.candidates.firstOrNull { it.id == pick.id }?.let { candidate ->
                    selectedCandidates[section.displayName] = candidate
                }
            }
        }
    }

    private fun renderSections(preserveScroll: Boolean) {
        val scrollY = (content.parent as? ScrollView)?.scrollY ?: 0
        content.removeAllViews()

        if (currentSections.isEmpty()) {
            renderMessage(getString(R.string.source_selection_empty, title), showRetry = true)
            return
        }

        currentSections.forEach { section ->
            content.addView(
                buildSection(section),
                LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT).apply { bottomMargin = dp(26) }
            )
        }
        if (preserveScroll) {
            (content.parent as? ScrollView)?.post { it.scrollTo(0, scrollY) }
        }
        updateDoneButton()
    }

    private fun renderError() {
        currentSections = emptyList()
        content.removeAllViews()
        renderMessage(getString(R.string.source_selection_error), showRetry = true)
    }

    private fun renderMessage(message: String, showRetry: Boolean) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(56), dp(32), dp(24))
        }
        container.addView(TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(MUTED_COLOR)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))

        if (showRetry) {
            val button = TextView(this).apply {
                text = getString(R.string.retry)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = roundedBackground(ACCENT_COLOR)
                setPadding(dp(28), dp(12), dp(28), dp(12))
                setOnClickListener { loadCandidates() }
            }
            container.addView(button, LinearLayout.LayoutParams(LINEAR_WRAP_CONTENT, LINEAR_WRAP_CONTENT).apply {
                topMargin = dp(22)
            })
        }
        content.addView(container, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
    }

    private fun buildSkeletonSection(): View {
        val section = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        section.addView(TextView(this).apply {
            text = getString(R.string.source_selection_loading)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(MUTED_COLOR)
        }, linearMargins(left = 32))
        section.addView(
            View(this).apply { background = roundedBackground(SURFACE_COLOR) },
            linearMargins(top = 16, right = 32, height = 170)
        )
        return section
    }

    private fun buildSection(section: SourceCandidateSection): View {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(32), 0, dp(32), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "${section.displayName}  ·  ${section.candidates.size}"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, LINEAR_WRAP_CONTENT, 1f))

        if (singleServerType == null) {
            val isSkipped = section.displayName in skippedProviders
            header.addView(createBarButton(if (isSkipped) R.string.source_selection_undo else R.string.skip) {
                toggleSkip(section.displayName)
            })
        }
        container.addView(header, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            alpha = if (section.displayName in skippedProviders) 0.35f else 1f
        }
        section.candidates.forEachIndexed { index, candidate ->
            row.addView(
                buildCandidateCard(
                    label = section.displayName,
                    candidate = candidate,
                    isBest = index == 0,
                    clickable = section.displayName !in skippedProviders
                )
            )
        }
        val horizontalScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(32), 0, dp(32), 0)
        }
        horizontalScroll.addView(row, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
        container.addView(horizontalScroll, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, dp(236)))
        return container
    }

    private fun buildCandidateCard(label: String, candidate: SourceCandidate, isBest: Boolean, clickable: Boolean): View {
        val isSelected = selectedCandidates[label]?.id == candidate.id
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                SURFACE_COLOR,
                corner = 18,
                strokeColor = if (isSelected) ACCENT_COLOR else Color.TRANSPARENT,
                strokeWidthDp = if (isSelected) 3 else 0
            )
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(candidate.thumbnailUrl.takeIf(String::isNotEmpty)) { crossfade(true) }
        }
        card.addView(image, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, dp(158)))

        val titleView = TextView(this).apply {
            text = buildString {
                if (isSelected) append("✓ ")
                append(candidate.title.ifBlank { getString(R.string.unknown) })
            }
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(titleView, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, 0, 1f))

        if (isBest) {
            val badge = TextView(this).apply {
                text = getString(R.string.source_selection_best)
                textSize = 10f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = roundedBackground(ACCENT_COLOR, corner = 100)
            }
            card.addView(badge, linearMargins(left = 10, right = 10, bottom = 10, height = 24))
        }

        if (clickable) {
            card.setOnClickListener {
                if (singleServerType != null) {
                    pickSingleServer(label, candidate)
                } else {
                    toggleSelection(label, candidate)
                }
            }
        }
        card.layoutParams = LinearLayout.LayoutParams(dp(126), dp(220)).apply { rightMargin = dp(12) }
        return card
    }

    private fun toggleSelection(label: String, candidate: SourceCandidate) {
        if (label in skippedProviders) return
        val current = selectedCandidates[label]
        if (current?.id == candidate.id) selectedCandidates.remove(label) else selectedCandidates[label] = candidate
        renderSections(preserveScroll = true)
    }

    private fun toggleSkip(label: String) {
        if (label in skippedProviders) {
            skippedProviders.remove(label)
        } else {
            skippedProviders.add(label)
            selectedCandidates.remove(label)
        }
        renderSections(preserveScroll = true)
    }

    private fun pickSingleServer(label: String, candidate: SourceCandidate) {
        val id = aniListId.toString()
        sourceMappingStore.save(
            SourceMapping(
                aniListId = id,
                picks = mapOf(label to SourcePick(candidate.id, candidate.title, candidate.thumbnailUrl)),
                confirmedAt = System.currentTimeMillis()
            )
        )
        hasExistingMapping = true
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_RESULT_SERVER_TYPE, label)
            putExtra(EXTRA_RESULT_PICKED_ID, candidate.id)
            putExtra(EXTRA_RESULT_PICKED_TITLE, candidate.title)
            putExtra(EXTRA_RESULT_PICKED_THUMBNAIL, candidate.thumbnailUrl)
        })
        Toast.makeText(this, R.string.source_selection_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveAndFinishIfComplete(showToastWhenIncomplete: Boolean) {
        if (!isSelectionComplete()) {
            if (showToastWhenIncomplete) {
                Toast.makeText(this, R.string.source_selection_incomplete, Toast.LENGTH_SHORT).show()
            }
            return
        }
        saveMapping(selectedCandidates)
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_RESULT_MAPPING_SAVED, true)
        })
        Toast.makeText(this, R.string.source_selection_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveMapping(picks: Map<String, SourceCandidate>) {
        val id = aniListId.toString()
        sourceMappingStore.replace(
            SourceMapping(
                aniListId = id,
                picks = picks.mapValues { (_, candidate) ->
                    SourcePick(candidate.id, candidate.title, candidate.thumbnailUrl)
                },
                skipped = skippedProviders.toSet(),
                confirmedAt = System.currentTimeMillis()
            )
        )
        hasExistingMapping = picks.isNotEmpty()
    }

    private fun isSelectionComplete(): Boolean {
        val required = currentSections.mapTo(LinkedHashSet(), SourceCandidateSection::displayName)
        required.removeAll(skippedProviders)
        return required.isNotEmpty() && selectedCandidates.keys.containsAll(required)
    }

    private fun updateDoneButton() {
        val complete = isSelectionComplete()
        doneButton?.apply {
            background = roundedBackground(if (complete) ACCENT_COLOR else SURFACE_COLOR, corner = 100)
            setTextColor(if (complete) Color.WHITE else MUTED_COLOR)
        }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.source_selection_reset)
            .setMessage(R.string.source_selection_reset_message)
            .setPositiveButton(R.string.source_selection_reset) { _, _ -> resetMapping() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resetMapping() {
        aniListId?.let(sourceMappingStore::clear)
        selectedCandidates.clear()
        skippedProviders.clear()
        savedPicks.clear()
        hasExistingMapping = false
        resetButton?.visibility = View.GONE
        loadCandidates()
    }

    private fun createBarButton(textResource: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = getString(textResource)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(MUTED_COLOR)
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = roundedBackground(SURFACE_COLOR, corner = 100)
            setOnClickListener { onClick() }
        }
    }

    private fun roundedBackground(
        color: Int,
        corner: Int = 14,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidthDp: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(corner).toFloat()
            if (strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun linearMargins(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
        width: Int = LINEAR_MATCH_PARENT,
        height: Int = LINEAR_WRAP_CONTENT
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, height).apply {
            setMargins(dp(left), dp(top), dp(right), dp(bottom))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    internal companion object {
        const val INVALID_ANILIST_ID = -1
        const val LINEAR_MATCH_PARENT = -1
        const val LINEAR_WRAP_CONTENT = -2
        val PLAYER_BACKGROUND_COLOR = Color.parseColor("#0B1622")
        val SURFACE_COLOR = Color.parseColor("#151F2E")
        val MUTED_COLOR = Color.parseColor("#9FADBD")
        val ACCENT_COLOR = Color.parseColor("#F25560")

        const val EXTRA_TITLE = "EXTRA_SOURCE_SELECTION_TITLE"
        const val EXTRA_ANILIST_ID = "EXTRA_SOURCE_SELECTION_ANILIST_ID"
        const val EXTRA_SINGLE_SERVER_TYPE = "EXTRA_SOURCE_SELECTION_SINGLE_SERVER_TYPE"
        const val EXTRA_RESULT_SERVER_TYPE = "EXTRA_RESULT_SOURCE_SERVER_TYPE"
        const val EXTRA_RESULT_PICKED_ID = "EXTRA_RESULT_SOURCE_PICKED_ID"
        const val EXTRA_RESULT_PICKED_TITLE = "EXTRA_RESULT_SOURCE_PICKED_TITLE"
        const val EXTRA_RESULT_PICKED_THUMBNAIL = "EXTRA_RESULT_SOURCE_PICKED_THUMBNAIL"
        const val EXTRA_RESULT_MAPPING_SAVED = "EXTRA_RESULT_SOURCE_MAPPING_SAVED"
    }
}
