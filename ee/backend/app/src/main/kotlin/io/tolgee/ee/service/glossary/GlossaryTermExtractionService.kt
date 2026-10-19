package io.tolgee.ee.service.glossary

import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.KeyTranslationText
import io.tolgee.ee.data.glossary.GlossaryTermCandidatesResponse
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.repository.KeyRepository
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GlossaryTermExtractionService(
  private val keyRepository: KeyRepository,
  private val glossaryService: GlossaryService,
  private val glossaryTermService: GlossaryTermService,
  private val candidateExtractor: GlossaryTermCandidateExtractor,
  private val projectService: ProjectService,
) {
  @Transactional(readOnly = true)
  fun extract(
    projectId: Long,
    glossaryId: Long,
  ): GlossaryTermCandidatesResponse {
    val project: Project = projectService.get(projectId)
    val sourceLanguage = project.baseLanguage ?: throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
    val glossary =
      glossaryService
        .findAssignedToProject(project.organizationOwner.id, project.id)
        .firstOrNull { it.id == glossaryId }
        ?: throw NotFoundException(Message.GLOSSARY_NOT_FOUND)

    if (glossary.baseLanguageTag != sourceLanguage.tag) {
      throw BadRequestException(Message.GLOSSARY_BASE_LANGUAGE_MISMATCH)
    }
    if (sourceLanguage.tag
        .replace('_', '-')
        .substringBefore('-')
        .lowercase() in UNSUPPORTED_LANGUAGES
    ) {
      throw BadRequestException(Message.GLOSSARY_TERM_EXTRACTION_LANGUAGE_NOT_SUPPORTED)
    }

    val sourceTexts: List<KeyTranslationText> =
      keyRepository
        .getTranslationsForTermExtraction(project.id, sourceLanguage.id)
        .filter { it.text.isNotBlank() }
    val existingTerms =
      glossaryTermService
        .findAllWithTranslations(glossary)
        .flatMap { term -> term.translations.filter { it.languageTag == glossary.baseLanguageTag }.map { it.text } }
        .toSet()

    return GlossaryTermCandidatesResponse(
      candidateExtractor.extract(sourceTexts, existingTerms, sourceLanguage.tag),
    )
  }

  private companion object {
    val UNSUPPORTED_LANGUAGES = setOf("ja", "zh", "th", "lo", "km", "my")
  }
}
