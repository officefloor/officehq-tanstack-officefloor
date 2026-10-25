import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { openingsImportPath, periodsQueryKey } from './periods';

// The opening balances paste box: each line is name,period,amount (a person on the sheet by name or alias, a period
// on the sheet, an amount 0.00 or more with up to two decimals) and adds its amount to the person's carried figure for
// that period. Every line is checked first: one refused line changes nothing. How many lines went through shows once
// done, and is gone while it runs.
function ImportOpenings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const paste = useMutation({
    mutationFn: (body: { text: string }) => postJson<{ added: number }>(openingsImportPath(sheet ?? ''), body, user),
    // Not awaited: the count shows before the carried figures refresh.
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(paste);
  if (!user || !sheet) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit({ text });
      }}
    >
      <h2>Opening balances</h2>
      <textarea
        data-testid="opening-import"
        placeholder="name,period,amount — one opening balance per line"
        rows={3}
        value={text}
        onChange={(e) => { setText(e.target.value); paste.reset(); }}
      />
      <button type="submit" data-testid="opening-import-submit" disabled={paste.isPending}>
        Add opening balances
      </button>
      {paste.isSuccess && (
        <>
          {' '}Added <span data-testid="opening-import-done">{paste.data.added}</span>
        </>
      )}
      {paste.isError && (
        <span data-testid="opening-error">
          Nothing was added: every line needs a person on the sheet (by name or alias), a period on the sheet and an
          amount, 0.00 or more, with up to two decimals.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 16, Component: ImportOpenings });
