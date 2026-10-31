import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ratesKey } from './useRates';

// The rate import box: one rate per line as grade,rate,from,to (the dates may be absent or blank).
// Every line is checked first by the rate form's rules; one refused line sets nothing. Otherwise each
// line is set in order as the rate form would, replacing a rate for the same grade and from-date. Then
// shows how many were set; pressing again removes the old count until the new one is in.
export function ImportRates() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const importRates = useMutation({
    mutationFn: (pasted: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-rates`, { text: pasted, user }),
    onSuccess: () => {
      setText('');
      // Not awaited: the count shows as soon as the import is in.
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheet).slice(0, 3) });
    },
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importRates.reset();
          return importRates.mutateAsync(text);
        });
      }}
    >
      <textarea
        data-testid="import-rates"
        value={text}
        rows={3}
        placeholder="grade,rate,from,to — one rate per line"
        onChange={(e) => {
          setText(e.target.value);
          importRates.reset();
        }}
      />{' '}
      <button type="submit" data-testid="import-rates-submit">Import rates</button>
      {importRates.isSuccess && (
        <>
          {' '}
          Set <span data-testid="import-rates-set">{importRates.data.set}</span>
        </>
      )}
      {importRates.isError && (
        <span data-testid="import-rates-error" role="alert">
          {' '}
          Nothing was set: every line needs one of the sheet's grades, a percent of 0.00 or more with at most two decimals,
          from a valid date (YYYY-MM-DD) or none, to a valid date not before it or none.
        </span>
      )}
    </form>
  );
}
