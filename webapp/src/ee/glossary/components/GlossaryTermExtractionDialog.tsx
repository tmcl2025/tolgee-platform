import {
  Alert,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { useEffect, useState } from 'react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { components } from 'tg.service/apiSchema.generated';

type Candidate = components['schemas']['GlossaryTermCandidate'];

type ReviewedCandidate = Candidate & {
  accepted: boolean;
  description: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
};

export const GlossaryTermExtractionDialog = ({ open, onClose }: Props) => {
  const glossary = useGlossary();
  const { t } = useTranslate();
  const assignedProjects = glossary.assignedProjects;
  const [projectId, setProjectId] = useState<number | ''>(
    assignedProjects[0]?.id ?? ''
  );
  const [candidates, setCandidates] = useState<ReviewedCandidate[]>([]);
  const [hasExtracted, setHasExtracted] = useState(false);

  const extractionMutation = useApiMutation({
    url: '/v2/projects/{projectId}/glossaries/{glossaryId}/term-candidates',
    method: 'post',
  });
  const createMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/batch',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });

  useEffect(() => {
    if (open) {
      setProjectId(assignedProjects[0]?.id ?? '');
      setCandidates([]);
      setHasExtracted(false);
      extractionMutation.reset();
      createMutation.reset();
    }
    // Mutation objects are stable for the lifetime of this dialog.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const extract = async () => {
    if (projectId === '') return;
    try {
      const result = await extractionMutation.mutateAsync({
        path: {
          projectId,
          glossaryId: glossary.id,
        },
      });
      setCandidates(
        (result.candidates ?? []).map((candidate) => ({
          ...candidate,
          accepted: false,
          description: '',
        }))
      );
      setHasExtracted(true);
    } catch {
      // The mutation error is shown inline below.
    }
  };

  const accept = async () => {
    const terms = candidates
      .filter((candidate) => candidate.accepted)
      .map(({ term, description }) => ({ term, description }));
    if (!terms.length) return;

    try {
      const result = await createMutation.mutateAsync({
        path: {
          organizationId: glossary.organizationOwner.id,
          glossaryId: glossary.id,
        },
        content: {
          'application/json': { terms },
        },
      });
      messageService.success(
        <T
          keyName="glossary_term_extraction_created_message"
          params={{
            created: result.created,
            skipped: result.skippedDuplicates,
          }}
        />
      );
      onClose();
    } catch {
      // The mutation error is shown inline below.
    }
  };

  const updateCandidate = (
    index: number,
    change: Partial<ReviewedCandidate>
  ) => {
    setCandidates((current) =>
      current.map((candidate, i) =>
        i === index ? { ...candidate, ...change } : candidate
      )
    );
  };

  const error = extractionMutation.error ?? createMutation.error;
  const errorKey =
    error?.code === 'glossary_base_language_mismatch'
      ? 'glossary_term_extraction_language_mismatch'
      : error?.code === 'glossary_term_extraction_language_not_supported'
      ? 'glossary_term_extraction_language_unsupported'
      : 'glossary_term_extraction_error';

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <T keyName="glossary_term_extraction_title" />
      </DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" sx={{ mb: 2 }}>
          <T keyName="glossary_term_extraction_description" />
        </Typography>
        <FormControl fullWidth size="small" sx={{ mb: 2 }}>
          <InputLabel id="term-extraction-project-label">
            <T keyName="glossary_term_extraction_project_label" />
          </InputLabel>
          <Select
            labelId="term-extraction-project-label"
            value={projectId}
            label={t('glossary_term_extraction_project_label')}
            onChange={(event) => {
              setProjectId(event.target.value as number);
              setCandidates([]);
              setHasExtracted(false);
              extractionMutation.reset();
            }}
            data-cy="glossary-term-extraction-project-select"
          >
            {assignedProjects.map((project) => (
              <MenuItem key={project.id} value={project.id}>
                {project.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <Button
          onClick={extract}
          disabled={projectId === '' || extractionMutation.isLoading}
          variant="contained"
          data-cy="glossary-term-extraction-run"
        >
          <T keyName="glossary_term_extraction_run" />
        </Button>

        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            <T keyName={errorKey} />
          </Alert>
        )}

        {hasExtracted && candidates.length === 0 && (
          <Alert severity="info" sx={{ mt: 2 }}>
            <T keyName="glossary_term_extraction_no_candidates" />
          </Alert>
        )}

        {candidates.length > 0 && (
          <TableContainer sx={{ mt: 2, maxHeight: '55vh' }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={
                        candidates.length > 0 &&
                        candidates.every((candidate) => candidate.accepted)
                      }
                      indeterminate={
                        candidates.some((candidate) => candidate.accepted) &&
                        candidates.some((candidate) => !candidate.accepted)
                      }
                      onChange={(event) =>
                        setCandidates((current) =>
                          current.map((candidate) => ({
                            ...candidate,
                            accepted: event.target.checked,
                          }))
                        )
                      }
                      inputProps={{
                        'aria-label': t(
                          'glossary_term_extraction_select_all_candidates'
                        ),
                      }}
                      data-cy="glossary-term-extraction-select-all"
                    />
                  </TableCell>
                  <TableCell>
                    <T keyName="glossary_term_extraction_term_header" />
                  </TableCell>
                  <TableCell>
                    <T keyName="glossary_term_extraction_frequency_header" />
                  </TableCell>
                  <TableCell>
                    <T keyName="glossary_term_extraction_examples_header" />
                  </TableCell>
                  <TableCell>
                    <T keyName="glossary_term_extraction_description_header" />
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {candidates.map((candidate, index) => (
                  <TableRow key={candidate.term} hover>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={candidate.accepted}
                        onChange={(event) =>
                          updateCandidate(index, {
                            accepted: event.target.checked,
                          })
                        }
                        inputProps={{
                          'aria-label': t(
                            'glossary_term_extraction_accept_candidate',
                            { term: candidate.term }
                          ),
                        }}
                        data-cy="glossary-term-extraction-candidate-checkbox"
                      />
                    </TableCell>
                    <TableCell sx={{ minWidth: 150, fontWeight: 600 }}>
                      {candidate.term}
                    </TableCell>
                    <TableCell sx={{ minWidth: 90 }}>
                      {candidate.occurrenceCount}{' '}
                      <T keyName="glossary_term_extraction_occurrences_short" />
                      <br />
                      {candidate.keyCount}{' '}
                      <T keyName="glossary_term_extraction_keys_short" />
                    </TableCell>
                    <TableCell sx={{ minWidth: 280, maxWidth: 420 }}>
                      {candidate.examples.map((example) => (
                        <Typography
                          key={example}
                          variant="body2"
                          sx={{ mb: 0.5, overflowWrap: 'anywhere' }}
                        >
                          {example}
                        </Typography>
                      ))}
                    </TableCell>
                    <TableCell sx={{ minWidth: 220 }}>
                      <TextField
                        fullWidth
                        size="small"
                        multiline
                        minRows={1}
                        maxRows={3}
                        value={candidate.description}
                        onChange={(event) =>
                          updateCandidate(index, {
                            description: event.target.value,
                          })
                        }
                        inputProps={{ maxLength: 500 }}
                        data-cy="glossary-term-extraction-description"
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} data-cy="glossary-term-extraction-cancel">
          <T keyName="glossary_term_extraction_cancel" />
        </Button>
        <Button
          onClick={accept}
          variant="contained"
          disabled={
            !candidates.some((candidate) => candidate.accepted) ||
            createMutation.isLoading
          }
          data-cy="glossary-term-extraction-accept"
        >
          <T
            keyName="glossary_term_extraction_add_selected"
            params={{ count: candidates.filter((c) => c.accepted).length }}
          />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
