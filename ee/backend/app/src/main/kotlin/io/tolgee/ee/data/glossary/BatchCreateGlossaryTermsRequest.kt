package io.tolgee.ee.data.glossary

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class BatchCreateGlossaryTermsRequest(
  @field:Valid
  @field:Size(min = 1, max = 500)
  var terms: List<BatchCreateGlossaryTermRequest> = emptyList(),
)

class BatchCreateGlossaryTermRequest(
  @field:NotBlank
  @field:Size(max = 50)
  var term: String = "",
  @field:Size(max = 500)
  var description: String = "",
)

data class BatchCreateGlossaryTermsResponse(
  val created: Int,
  val skippedDuplicates: Int,
)
