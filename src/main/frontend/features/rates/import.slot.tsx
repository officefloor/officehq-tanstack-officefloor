import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { ratesQueryKey } from './rates';

// The rate import box: each line is grade,rate,from,to (the from-date and the to-date may be blank or absent) and
// sets that grade's rate, in order, exactly as the rate form would — replacing the grade's rate with the same
// from-date. The server checks every line by the rate form's rules first, so one refused line sets nothing. How many
// were set shows once done, and is gone while it runs.
function ImportRates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const importRates = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/import-rates`, body, user),
    // Not awaited: the count shows before the new rates do.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(importRates);
  if (!user || !sheet) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit({ text });
      }}
    >
      <h2>Import rates</h2>
      <textarea
        data-testid="import-rates"
        placeholder="grade,rate,from,to — one rate per line"
        rows={4}
        value={text}
        onChange={(e) => { setText(e.target.value); importRates.reset(); }}
      />
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>
      {importRates.isSuccess && (
        <>
          {' '}Set <span data-testid="import-rates-set">{importRates.data.set}</span>
        </>
      )}
      {importRates.isError && (
        <span data-testid="import-rates-error">
          Nothing was set: every line needs a grade of A, B or C and a rate that is a percent, 0.00 or more, with up to
          two decimals; a from-date or to-date must be a calendar date or blank, and the to-date not before the
          from-date.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 5.5, Component: ImportRates });
