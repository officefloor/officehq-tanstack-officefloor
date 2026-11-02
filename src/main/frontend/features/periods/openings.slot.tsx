import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { openingsPath, periodsKey } from './periods';
import type { Period } from './periods';

type ImportedOpenings = { set: number; periods: Period[] };

// The opening-balance paste: lines of name,period,amount (a person on the sheet by name or alias, a period on the
// sheet, an amount of two decimals, 0.00 or more). The server checks every line first, so a refusal sets nothing;
// each line adds its amount to the person's carried figure for the period. Shows how many lines went through.
function ImportOpenings({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (body: string) => postJson<ImportedOpenings>(byUser(openingsPath(sheet), user), { text: body }),
    onSuccess: (result) => {
      queryClient.setQueryData(periodsKey(sheet), result.periods);
      setText('');
    },
  });
  const send = useSubmitOnce(importing);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(text);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Paste opening balances (name,period,amount per line)
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
      <button type="submit" data-testid="opening-import-submit" disabled={importing.isPending}>
        Paste openings
      </button>
      {importing.isSuccess && (
        <>
          {' '}Set <strong data-testid="opening-import-done">{importing.data.set}</strong>
        </>
      )}
      {importing.isError && (
        <span data-testid="opening-error" role="alert">
          {' '}Nothing was set: every line needs a person on the sheet (by name or alias), a period on the sheet, and an
          amount of two decimals, 0.00 or more.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 25.5, Component: ImportOpenings });
