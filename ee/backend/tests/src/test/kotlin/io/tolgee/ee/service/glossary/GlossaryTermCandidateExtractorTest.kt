package io.tolgee.ee.service.glossary

import io.tolgee.dtos.queryResults.KeyTranslationText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlossaryTermCandidateExtractorTest {
  private val extractor = GlossaryTermCandidateExtractor()

  @Test
  fun `ranks repeated phrases and reports occurrences keys and examples`() {
    val results =
      extractor.extract(
        listOf(
          KeyTranslationText(1, "The billing account is ready. Billing account settings are open."),
          KeyTranslationText(2, "Review the billing account before continuing."),
        ),
        emptySet(),
        "en",
      )

    val candidate = results.single { it.term.equals("billing account", ignoreCase = true) }
    assertEquals(3, candidate.occurrenceCount)
    assertEquals(2, candidate.keyCount)
    assertEquals(2, candidate.examples.size)
    assertTrue(results.none { it.term == "the" })
    assertTrue(results.none { it.term.startsWith("the ") })
  }

  @Test
  fun `normalizes case and unicode and omits existing glossary terms`() {
    val results =
      extractor.extract(
        listOf(
          KeyTranslationText(1, "Billing Account is ready."),
          KeyTranslationText(2, "billing account settings."),
          KeyTranslationText(3, "Ａccount billing is available."),
          KeyTranslationText(4, "Account billing settings."),
        ),
        setOf("BILLING ACCOUNT"),
        "en",
      )

    assertFalse(results.any { it.term.equals("billing account", ignoreCase = true) })
    assertTrue(results.any { it.term.equals("account billing", ignoreCase = true) })
  }

  @Test
  fun `ignores single occurrence candidates and limits phrase length`() {
    val results =
      extractor.extract(
        listOf(
          KeyTranslationText(1, "alpha beta gamma delta epsilon zeta unique"),
          KeyTranslationText(2, "alpha beta gamma delta epsilon zeta"),
        ),
        emptySet(),
        "en",
      )

    assertFalse(results.any { it.term == "unique" })
    assertFalse(results.any { it.term == "alpha beta gamma delta epsilon zeta" })
    assertTrue(results.any { it.term == "alpha beta gamma delta epsilon" })
  }
}
