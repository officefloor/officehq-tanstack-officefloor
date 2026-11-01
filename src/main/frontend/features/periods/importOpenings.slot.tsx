import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { periodsKey } from './usePeriods';

/**
 * Paste lines of name,period,amount (a person on the sheet by name or alias, a period on the sheet,
 * an amount of two decimals, 0.00 or more): each adds its amount to the person's carried figure for
 * that period. Every line is checked first: one refused line refuses the whole paste. Shows how many
 * lines went through; the count goes while the next press runs.
 */
function ImportOpenings({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (t: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheet)}/import-openings`, { text: t, user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  return (
    <section data-testid="opening-import-panel">
      <label>
        Opening balances (name,period,amount per line)
        <br />
        <textarea
          data-testid="opening-import"
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
        data-testid="opening-import-submit"
        disabled={importing.isPending}
        onClick={() => {
          importing.reset();
          importing.mutate(text);
        }}
      >
        Import openings
      </button>
      {importing.isSuccess && (
        <span>
          {' '}
          Set <strong data-testid="opening-import-done">{importing.data.set}</strong>
        </span>
      )}
      {importing.isError && (
        <span data-testid="opening-error" role="alert">
          {' '}
          Nothing was set:{' '}
          {importing.error instanceof HttpError && importing.error.body
            ? importing.error.body
            : 'every line needs a person and a period on the sheet and an amount of 0.00 or more.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6.5, Component: ImportOpenings });
