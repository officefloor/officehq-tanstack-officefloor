import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { periodsKey } from '../periods/periods';

type Result = { imported: number };

/** The opening balances box: lines of name,period,amount (a person on the sheet by name or alias, a period on the
 *  sheet, an amount of 0.00 or more with two decimals). The server checks every line first: one failing line refuses
 *  the paste and stores nothing. Each line adds its amount to the person's carried figure for the period. Shows how
 *  many lines went through; the count is cleared while a new press runs. */
function ImportOpenings({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const submit = useSubmitOnce();
  const importOpenings = useMutation({
    mutationFn: (t: string) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/import-openings`, { text: t, by: user }),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!submit.begin()) return;
        importOpenings.reset();
        importOpenings.mutate(text);
      }}
    >
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
            importOpenings.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="opening-import-submit" disabled={importOpenings.isPending}>
        Paste opening balances
      </button>
      {importOpenings.isSuccess && (
        <>
          {' '}
          Took <span data-testid="opening-import-done">{importOpenings.data.imported}</span>
        </>
      )}
      {importOpenings.isError && (
        <span data-testid="opening-error">
          {' '}
          Nothing was pasted: every line must name a person (or alias) and a period on the sheet, with an amount of 0.00
          or more with two decimals.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 12.5, Component: ImportOpenings });
