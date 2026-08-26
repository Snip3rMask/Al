package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.SourceCandidateSection
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.SourceCandidateRepository

class DefaultSourceCandidateRepository(
    private val providers: List<SourceProvider>
) : SourceCandidateRepository {

    override fun findCandidates(
        title: String,
        aniListId: Int?,
        singleServerType: String?
    ): Single<List<SourceCandidateSection>> {
        return Observable.fromIterable(providers.withIndex().toList())
            .flatMapSingle { (providerIndex, provider) ->
                provider.findCandidateGroups(title, aniListId)
                    .map<ProviderResult> { ProviderResult(providerIndex, provider.id, it, null) }
                    .onErrorResumeNext { error ->
                        Single.just(ProviderResult(providerIndex, provider.id, emptyMap(), error))
                    }
            }
            .toList()
            .map { results -> mergeResults(results.sortedBy(ProviderResult::index)) }
            .flatMap { sections ->
                if (sections.isEmpty() && requestedProviderCount(singleServerType) > 0) {
                    Single.error(IllegalStateException("No source candidate providers succeeded"))
                } else {
                    Single.just(visibleSections(sections, singleServerType))
                }
            }
    }

    private fun mergeResults(results: List<ProviderResult>): List<SourceCandidateSection> {
        if (results.all { it.error != null }) return emptyList()

        val merged = LinkedHashMap<String, SourceCandidateSection>()
        results.forEach { result ->
            result.groups.forEach { (label, candidates) ->
                if (candidates.isEmpty()) return@forEach
                val existing = merged.getOrPut(label) {
                    SourceCandidateSection(result.providerId, label, emptyList())
                }
                val seen = existing.candidates.mapTo(mutableSetOf(), SourceCandidate::id)
                val unique = candidates.filter { candidate ->
                    candidate.id.isNotBlank() && seen.add(candidate.id)
                }
                merged[label] = existing.copy(candidates = existing.candidates + unique)
            }
        }
        return merged.values.filter { it.candidates.isNotEmpty() }
    }

    private fun requestedProviderCount(singleServerType: String?): Int {
        return singleServerType?.trim()?.takeIf(String::isNotEmpty)?.let { 1 } ?: providers.size
    }

    private fun visibleSections(
        sections: List<SourceCandidateSection>,
        singleServerType: String?
    ): List<SourceCandidateSection> {
        val requested = singleServerType?.trim().orEmpty()
        if (requested.isEmpty()) return sections
        return sections.filter {
            it.displayName.equals(requested, ignoreCase = true) || it.providerId.equals(requested, ignoreCase = true)
        }
    }

    private data class ProviderResult(
        val index: Int,
        val providerId: String,
        val groups: Map<String, List<SourceCandidate>>,
        val error: Throwable?
    )
}
