package msr.atsulab.app.player.domain.model

data class SourceCandidateSection(
    val providerId: String,
    val displayName: String,
    val candidates: List<SourceCandidate>
)
