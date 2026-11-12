import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { ratesKey } from './rates';

type ImportResult = { set: number };

// The rate import box: lines of grade,rate,from,to each set a rate as the rate form would (the from and
// to may be absent or blank). The server checks every line with the rate form's rules first and refuses
// the whole import if any fails; the count set shows once it answers (gone while it runs).
function ImportRates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const importRates = useMutation({
    mutationFn: (t: string) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheet)}/import-rates`, { text: t, user }),
    // Not awaited: the count set shows as soon as the server answers.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
    },
  });
  const submit = useSubmitOnce(importRates);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit(text);
      }}
    >
      <h2>Import rates</h2>
      <textarea
        data-testid="import-rates"
        placeholder="grade,rate,from,to — one rate per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          importRates.reset();
        }}
      />
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>
      {importRates.isSuccess && (
        <>
          {' '}
          Set <strong data-testid="import-rates-set">{importRates.data.set}</strong>
        </>
      )}
      {importRates.isError && (
        <span data-testid="import-rates-error" role="alert">
          {' '}
          {importRates.error instanceof HttpError && importRates.error.reason
            ? `${importRates.error.reason}.`
            : 'The import was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 41, Component: ImportRates });
