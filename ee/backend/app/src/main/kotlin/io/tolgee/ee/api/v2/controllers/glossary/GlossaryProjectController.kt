package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.SimpleGlossaryModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryModel
import io.tolgee.ee.data.glossary.GlossaryTermCandidatesResponse
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.ee.service.glossary.GlossaryTermExtractionService
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/glossaries")
@Tag(name = "Glossary")
class GlossaryProjectController(
  private val projectHolder: ProjectHolder,
  private val glossaryService: GlossaryService,
  private val simpleGlossaryModelAssembler: SimpleGlossaryModelAssembler,
  private val glossaryTermExtractionService: GlossaryTermExtractionService,
) {
  @GetMapping
  @Operation(summary = "Get glossaries assigned to project")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.GLOSSARY)
  fun getAssignedGlossaries(): CollectionModel<SimpleGlossaryModel> {
    val project = projectHolder.project
    val organizationId = project.organizationOwnerId
    val glossaries = glossaryService.findAssignedToProject(organizationId, project.id)
    return simpleGlossaryModelAssembler.toCollectionModel(glossaries)
  }

  @PostMapping("/{glossaryId:[0-9]+}/term-candidates")
  @Operation(summary = "Extract glossary term candidates from project source strings")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.GLOSSARY)
  fun extractTermCandidates(
    @PathVariable projectId: Long,
    @PathVariable glossaryId: Long,
  ): GlossaryTermCandidatesResponse {
    if (projectHolder.project.id != projectId) {
      throw NotFoundException(Message.PROJECT_NOT_FOUND)
    }
    return glossaryTermExtractionService.extract(projectId, glossaryId)
  }
}
