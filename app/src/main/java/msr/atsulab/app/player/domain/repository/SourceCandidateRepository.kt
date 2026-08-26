package msr.atsulab.app.player.domain.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.SourceCandidateSection

interface SourceCandidateRepository {
    fun findCandidates(
        title: String,
        aniListId: Int?,
        singleServerType: String? = null
    ): Single<List<SourceCandidateSection>>
}
