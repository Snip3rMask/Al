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
import androidx.appcompat.app.AppCompatActivity
import coil.load
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.SourceCandidateSection
import msr.atsulab.app.player.domain.repository.SourceCandidateRepository
import org.koin.java.KoinJavaComponent.inject

class SourceSelectionActivity : AppCompatActivity() {

    private val sourceCandidateRepository: SourceCandidateRepository by inject(SourceCandidateRepository::class.java)

    private lateinit var content: LinearLayout
    private var disposable: Disposable? = null
    private var title: String = ""
    private var aniListId: Int? = null
    private var singleServerType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, INVALID_ANILIST_ID)
            .takeIf { it != INVALID_ANILIST_ID }
        singleServerType = intent.getStringExtra(EXTRA_SINGLE_SERVER_TYPE)?.takeIf(String::isNotEmpty)

        if (title.isBlank()) {
            Toast.makeText(this, R.string.source_selection_missing_title, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(buildView())
        window.statusBarColor = Color.parseColor("#0B1622")
        window.navigationBarColor = Color.parseColor("#0B1622")
        loadCandidates()
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    private fun buildView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B1622"))
        }
        root.addView(buildTopBar(), LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))

        if (singleServerType == null) {
            val instruction = TextView(this).apply {
                text = getString(R.string.source_selection_instruction)
                textSize = 14f
                setTextColor(Color.parseColor("#9FADBD"))
                gravity = Gravity.CENTER
                setPadding(dp(32), 0, dp(32), dp(12))
            }
            root.addView(instruction, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
        }

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setFillViewport(true)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content, FrameLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, 0, 1f))
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
        bar.addView(label, LinearLayout.LayoutParams(0, LINEAR_WRAP_CONTENT, 1f).apply {
            leftMargin = dp(8)
        })
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

    private fun renderSuccess(sections: List<SourceCandidateSection>) {
        content.removeAllViews()
        if (sections.isEmpty()) {
            renderMessage(getString(R.string.source_selection_empty, title), showRetry = true)
            return
        }
        sections.forEach { section ->
            content.addView(buildSection(section), LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT).apply {
                bottomMargin = dp(26)
            })
        }
    }

    private fun renderError() {
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
            setTextColor(Color.parseColor("#9FADBD"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, LINEAR_WRAP_CONTENT))

        if (showRetry) {
            val button = TextView(this).apply {
                text = getString(R.string.retry)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = roundedBackground(Color.parseColor("#F25560"))
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
            setTextColor(Color.parseColor("#9FADBD"))
        }, linearMargins(left = 32))
        section.addView(
            View(this).apply { background = roundedBackground(Color.parseColor("#151F2E")) },
            linearMargins(top = 16, right = 32, height = 170)
        )
        return section
    }

    private fun buildSection(section: SourceCandidateSection): View {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(this).apply {
            text = "${section.displayName}  ·  ${section.candidates.size}"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        container.addView(header, linearMargins(left = 32, right = 32, bottom = 14))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        section.candidates.forEachIndexed { index, candidate ->
            row.addView(buildCandidateCard(candidate.title, candidate.thumbnailUrl, index == 0))
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

    private fun buildCandidateCard(titleText: String, imageUrl: String, isBest: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(Color.parseColor("#151F2E"), corner = 18)
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(imageUrl.takeIf(String::isNotEmpty)) {
                crossfade(true)
            }
        }
        card.addView(image, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, dp(158)))

        val title = TextView(this).apply {
            text = titleText.ifBlank { getString(R.string.unknown) }
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(title, LinearLayout.LayoutParams(LINEAR_MATCH_PARENT, 0, 1f))

        if (isBest) {
            val badge = TextView(this).apply {
                text = getString(R.string.source_selection_best)
                textSize = 10f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = roundedBackground(Color.parseColor("#F25560"), corner = 100)
            }
            card.addView(badge, linearMargins(left = 10, right = 10, bottom = 10, height = 24, width = LINEAR_MATCH_PARENT))
        }
        return card.apply {
            layoutParams = LinearLayout.LayoutParams(dp(126), dp(220)).apply {
                rightMargin = dp(12)
            }
        }
    }

    private fun roundedBackground(color: Int, corner: Int = 14): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(corner).toFloat()
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

    private companion object {
        const val INVALID_ANILIST_ID = -1
        const val LINEAR_MATCH_PARENT = -1
        const val LINEAR_WRAP_CONTENT = -2

        fun start(context: android.content.Context, title: String, aniListId: Int?, singleServerType: String?) {
            context.startActivity(Intent(context, SourceSelectionActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                aniListId?.let { putExtra(EXTRA_ANILIST_ID, it) }
                singleServerType?.let { putExtra(EXTRA_SINGLE_SERVER_TYPE, it) }
            })
        }

        const val EXTRA_TITLE = "EXTRA_SOURCE_SELECTION_TITLE"
        const val EXTRA_ANILIST_ID = "EXTRA_SOURCE_SELECTION_ANILIST_ID"
        const val EXTRA_SINGLE_SERVER_TYPE = "EXTRA_SOURCE_SELECTION_SINGLE_SERVER_TYPE"
    }
}
