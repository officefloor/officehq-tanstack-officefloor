import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { ratesKey } from './useRates';

/**
 * Paste lines of grade,rate,from,to (the last two may be absent or blank): a rate per line, set in
 * order exactly as the rate form would set it. Every line is checked by the form's rules first: one
 * refused line refuses the whole import. Shows how many were set; the count goes while the next
 * press runs.
 */
function ImportRates({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (t: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-rates`, { text: t, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ratesKey(sheet) }),
  });
  return (
    <section data-testid="import-rates-panel">
      <label>
        Import rates (grade,rate,from,to per line)
        <br />
        <textarea
          data-testid="import-rates"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importing.reset();
          }}
        />
      </label>
      <br />
      <button
        type="button"
        data-testid="import-rates-submit"
        disabled={importing.isPending}
        onClick={() => {
          importing.reset();
          importing.mutate(text);
        }}
      >
        Import rates
      </button>
      {importing.isSuccess && (
        <span>
          {' '}
          Set <strong data-testid="import-rates-set">{importing.data.set}</strong>
        </span>
      )}
      {importing.isError && (
        <span data-testid="import-rates-error" role="alert">
          {' '}
          Nothing was set:{' '}
          {importing.error instanceof HttpError && importing.error.body
            ? importing.error.body
            : 'every line needs a rate the rate form would accept.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 5.5, Component: ImportRates });
