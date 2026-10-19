import React, { useRef, useState } from 'react';
import { useSelectionService } from 'tg.service/useSelectionService';
import { GlossaryViewTopbar } from 'tg.ee.module/glossary/components/GlossaryViewTopbar';
import { GlossaryViewToolbar } from 'tg.ee.module/glossary/components/GlossaryViewToolbar';
import { GlossaryTermsList } from 'tg.ee.module/glossary/components/GlossaryTermsList';
import { useSelectedGlossaryLanguages } from 'tg.ee.module/glossary/hooks/useSelectedGlossaryLanguages';
import { useGlossaryTerms } from 'tg.ee.module/glossary/hooks/useGlossaryTerms';
import { useGlossaryTermCreateDialogController } from 'tg.ee.module/glossary/hooks/useGlossaryTermCreateDialogController';
import { useGlossaryImportDialogController } from 'tg.ee.module/glossary/hooks/useGlossaryImportDialogController';
import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';
import { GlossaryImportDialog } from 'tg.ee.module/glossary/components/GlossaryImportDialog';
import { GlossaryTermExtractionDialog } from 'tg.ee.module/glossary/components/GlossaryTermExtractionDialog';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

type Props = {
  onSearch?: (search: string) => void;
  search?: string;
};

export const GlossaryViewBody: React.VFC<Props> = ({ onSearch, search }) => {
  const glossary = useGlossary();
  const [termExtractionOpen, setTermExtractionOpen] = useState(false);
  const verticalScrollRef = useRef<HTMLDivElement>(null);
  const clearSearchRef = useRef<(() => void) | null>(null);

  const [selectedLanguages, setSelectedLanguages] =
    useSelectedGlossaryLanguages();

  const { terms, total, loading, onFetchNextPageHint, getAllTermsIds } =
    useGlossaryTerms({
      search,
      languageTags: selectedLanguages,
    });

  const selectionService = useSelectionService<number>({
    totalCount: total,
    itemsAll: getAllTermsIds,
  });

  const {
    onCreateTerm,
    canCreate,
    createTermDialogOpen,
    createTermDialogProps,
  } = useGlossaryTermCreateDialogController();

  const hasExistingTerms = total !== undefined && total > 0;
  const { onImport, importDialogOpen, importDialogProps } =
    useGlossaryImportDialogController();

  return (
    <>
      {createTermDialogOpen && (
        <GlossaryTermCreateDialog {...createTermDialogProps} />
      )}
      {importDialogOpen && (
        <GlossaryImportDialog
          {...importDialogProps}
          hasExistingTerms={hasExistingTerms}
        />
      )}
      {termExtractionOpen && (
        <GlossaryTermExtractionDialog
          open={termExtractionOpen}
          onClose={() => setTermExtractionOpen(false)}
        />
      )}
      {terms && (
        <GlossaryViewTopbar
          onCreateTerm={onCreateTerm}
          onImport={onImport}
          onExtractTerms={
            canCreate && glossary.assignedProjects.length > 0
              ? () => setTermExtractionOpen(true)
              : undefined
          }
          onSearch={onSearch}
          search={search}
          selectedLanguages={selectedLanguages}
          setSelectedLanguages={setSelectedLanguages}
          clearSearchCallbackRef={clearSearchRef}
        />
      )}
      <GlossaryTermsList
        terms={terms}
        loading={loading}
        total={total}
        selectedLanguages={selectedLanguages}
        selectionService={selectionService}
        onCreateTerm={onCreateTerm}
        onImport={onImport}
        onFetchNextPageHint={onFetchNextPageHint}
        clearSearchRef={clearSearchRef}
        verticalScrollRef={verticalScrollRef}
        search={search}
      />
      <GlossaryViewToolbar
        leftOffset={verticalScrollRef.current?.getBoundingClientRect?.()?.left}
        selectionService={selectionService}
      />
    </>
  );
};
