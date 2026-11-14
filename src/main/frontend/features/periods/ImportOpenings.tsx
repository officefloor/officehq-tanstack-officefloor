import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

type ImportOpeningsResult = { added: number };

// The opening balance box: lines of name,period,amount, each adding its amount to the person's carried figure for
// the period. The server checks every line first, so one refused line adds nothing.
export function ImportOpenings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [added, setAdded] = useState<number | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const importOpenings = useMutation({
    mutationFn: (pasted: string) =>
      postJson<ImportOpeningsResult>(
        `/api/sheets/${encodeURIComponent(sheet)}/import-openings?user=${encodeURIComponent(user)}`,
        { text: pasted },
      ),
    onSuccess: async (result) => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
      setAdded(result.added);
    },
    onError: () =>
      setError('Nothing was added: every line must be a person and a period on the sheet and an amount of 0.00 or more, as name,period,amount.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setAdded(undefined);
    if (!text.trim()) {
      setError('Paste at least one line of name,period,amount.');
      return;
    }
    once(() => importOpenings.mutateAsync(text));
  };

  return (
    <form onSubmit={submit} data-testid="opening-import-form">
      <textarea
        data-testid="opening-import"
        placeholder="name,period,amount — one opening balance per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="opening-import-submit" disabled={importOpenings.isPending}>
        Import opening balances
      </button>
      {added !== undefined && (
        <>
          {' '}
          Added <span data-testid="opening-import-done">{added}</span>
        </>
      )}
      {error && <span data-testid="opening-error">{error}</span>}
    </form>
  );
}
