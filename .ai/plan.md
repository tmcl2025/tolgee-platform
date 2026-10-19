# Statistical term extraction for project glossaries

## Summary
Add a review workflow that finds frequent term candidates in a project’s source strings and lets users add selected candidates to a compatible assigned glossary. CAT tools such as memoQ use a candidate review step before adding accepted terms to a term base; Phrase exposes controls such as minimum frequency and maximum phrase length. This plan follows that review pattern while keeping the first version within the language and session limits you chose. ([memoQ extraction workflow](https://docs.memoq.com/current/en/Workspace/ribbons-preparation.html), [memoQ accepted terms](https://docs.memoq.com/current/en/Workspace/export-accepted-terms-to-term-base.html), [Phrase term extraction](https://support.phrase.com/hc/en-us/articles/10825557848220-Job-Tools-TMS))

## Implementation changes
- Add an **Extract terms** action in the project’s glossary context. Use the project’s source strings as the corpus. If multiple assigned glossaries match the source language, let the user choose one; if none match, direct them to assign or create a compatible glossary.
- Extract contiguous one-to-five-word candidates from space-delimited languages. Normalize case and Unicode, filter punctuation and supported-language stop words, rank by frequency, and omit terms already in the destination glossary.
- Show candidates with occurrence count, distinct-key count, and source-string examples. Users can accept or discard candidates and edit the glossary description before saving; extraction itself does not generate definitions or translations.
- Add a project-authorized candidate endpoint and a batch-accept endpoint. Batch acceptance creates glossary terms and their base-language entries, rechecks for duplicates, and uses the existing glossary feature and permission checks.
- Keep review state in the current screen only; no extraction sessions or migrations are needed.

## Test plan
- Verify ranking, normalization, stop-word filtering, examples, duplicate filtering, empty results, and the candidate limit.
- Verify only accepted candidates are saved, edited descriptions persist, and repeated submissions do not duplicate existing terms.
- Verify access rules, glossary assignment, and base-language matching; show a clear unsupported-language message for non-space-delimited scripts.
- Cover the review, discard, edit-description, and batch-accept flows in the frontend.

## Assumptions and defaults
- V1 scans all source strings in the current project; it does not scan translations, selected keys, external documents, or translation memories.
- V1 supports space-delimited source languages. Phrase documents a similar Latin-character limitation; broader script support would require a later tokenizer expansion. ([Phrase term extraction limits](https://support.phrase.com/hc/en-us/articles/10825557848220-Job-Tools-TMS))
- Glossary descriptions are human-written, and candidate review is temporary until accepted or dismissed.
