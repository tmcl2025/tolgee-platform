package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema

data class GlossaryTermCandidate(
  @Schema(description = "Suggested term in the project's source language")
  val term: String,
  @Schema(description = "Number of occurrences across source strings")
  val occurrenceCount: Int,
  @Schema(description = "Number of distinct keys containing the term")
  val keyCount: Int,
  @Schema(description = "Example source strings containing the term")
  val examples: List<String>,
)

data class GlossaryTermCandidatesResponse(
  val candidates: List<GlossaryTermCandidate>,
)
