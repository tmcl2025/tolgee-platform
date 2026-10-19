package io.tolgee.ee.service.glossary

import io.tolgee.dtos.queryResults.KeyTranslationText
import io.tolgee.ee.data.glossary.GlossaryTermCandidate
import org.springframework.stereotype.Component
import java.text.Normalizer
import java.util.Locale

@Component
class GlossaryTermCandidateExtractor {
  fun extract(
    sourceTexts: List<KeyTranslationText>,
    existingTerms: Set<String>,
    languageTag: String,
  ): List<GlossaryTermCandidate> {
    val locale = Locale.forLanguageTag(languageTag.replace('_', '-'))
    val stopWords = stopWords[locale.language].orEmpty()
    val existing = existingTerms.mapTo(mutableSetOf()) { normalize(it, locale) }
    val candidates = mutableMapOf<String, CandidateCounts>()

    sourceTexts.forEach { source ->
      sentenceRegex.split(source.text).forEach { sentence ->
        val tokens = tokenRegex.findAll(sentence).map { it.value }.toList()
        for (start in tokens.indices) {
          for (length in 1..MAX_WORDS) {
            val end = start + length
            if (end > tokens.size) break
            val words = tokens.subList(start, end)
            if (words.any { it.none(Char::isLetter) }) continue
            if (words.first().lowercase(locale) in stopWords || words.last().lowercase(locale) in stopWords) continue

            val term = words.joinToString(" ")
            if (term.length > MAX_TERM_LENGTH) continue
            val key = normalize(term, locale)
            if (key in existing) continue

            val counts = candidates.getOrPut(key) { CandidateCounts() }
            counts.occurrences++
            counts.keyIds.add(source.keyId)
            counts.surfaceForms[term] = (counts.surfaceForms[term] ?: 0) + 1
            if (counts.examples.size < EXAMPLE_LIMIT) counts.examples.add(source.text.take(MAX_EXAMPLE_LENGTH))
          }
        }
      }
    }

    return candidates
      .mapNotNull { (normalized, counts) ->
        if (counts.occurrences < MIN_OCCURRENCES) return@mapNotNull null
        val displayTerm =
          counts.surfaceForms.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()
            ?.key ?: normalized
        GlossaryTermCandidate(
          term = displayTerm,
          occurrenceCount = counts.occurrences,
          keyCount = counts.keyIds.size,
          examples = counts.examples.toList(),
        )
      }.sortedWith(
        compareByDescending<GlossaryTermCandidate> { it.occurrenceCount }
          .thenByDescending { it.keyCount }
          .thenByDescending { it.term.split(' ').size }
          .thenBy { normalize(it.term, locale) },
      ).take(CANDIDATE_LIMIT)
  }

  private fun normalize(
    value: String,
    locale: Locale,
  ): String = Normalizer.normalize(value, Normalizer.Form.NFKC).trim().lowercase(locale)

  private class CandidateCounts {
    var occurrences = 0
    val keyIds = mutableSetOf<Long>()
    val surfaceForms = mutableMapOf<String, Int>()
    val examples = linkedSetOf<String>()
  }

  private companion object {
    const val MIN_OCCURRENCES = 2
    const val MAX_WORDS = 5
    const val MAX_TERM_LENGTH = 50
    const val CANDIDATE_LIMIT = 500
    const val EXAMPLE_LIMIT = 3
    const val MAX_EXAMPLE_LENGTH = 300

    val tokenRegex = Regex("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}'’_-]*")
    val sentenceRegex = Regex("[.!?;:。！？\\r\\n]+")

    val stopWords =
      mapOf(
        "en" to
          setOf(
            "a",
            "an",
            "and",
            "are",
            "as",
            "at",
            "be",
            "by",
            "for",
            "from",
            "in",
            "is",
            "it",
            "of",
            "on",
            "or",
            "that",
            "the",
            "this",
            "to",
            "with",
          ),
        "de" to
          setOf(
            "aber",
            "als",
            "am",
            "an",
            "auf",
            "aus",
            "bei",
            "das",
            "der",
            "des",
            "die",
            "ein",
            "eine",
            "einer",
            "eines",
            "für",
            "im",
            "in",
            "ist",
            "mit",
            "oder",
            "und",
            "von",
            "zu",
          ),
        "es" to
          setOf(
            "a",
            "al",
            "como",
            "con",
            "de",
            "del",
            "el",
            "en",
            "es",
            "la",
            "las",
            "los",
            "o",
            "para",
            "por",
            "un",
            "una",
            "y",
          ),
        "fr" to
          setOf(
            "au",
            "aux",
            "avec",
            "ce",
            "ces",
            "dans",
            "de",
            "des",
            "du",
            "elle",
            "en",
            "et",
            "la",
            "le",
            "les",
            "ou",
            "par",
            "pour",
            "un",
            "une",
          ),
        "it" to
          setOf(
            "a",
            "al",
            "alla",
            "con",
            "da",
            "dei",
            "del",
            "della",
            "di",
            "e",
            "gli",
            "il",
            "in",
            "la",
            "le",
            "o",
            "per",
            "un",
            "una",
          ),
        "nl" to
          setOf(
            "aan",
            "als",
            "bij",
            "dan",
            "dat",
            "de",
            "der",
            "een",
            "en",
            "het",
            "in",
            "met",
            "of",
            "om",
            "op",
            "te",
            "van",
            "voor",
          ),
        "pt" to
          setOf(
            "a",
            "ao",
            "com",
            "da",
            "das",
            "de",
            "do",
            "dos",
            "e",
            "em",
            "na",
            "nas",
            "no",
            "nos",
            "o",
            "os",
            "ou",
            "para",
            "por",
            "um",
            "uma",
          ),
      )
  }
}
