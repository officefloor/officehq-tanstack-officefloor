import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { periodsKey } from './periods';

type Result = { added: number };

// The opening balance paste box: lines of name,period,amount (a person on the sheet by name or alias, a
// period on the sheet, an amount of two decimals, 0.00 or more) each add their amount to the person's
// carried figure for the period. The server checks every line first: one bad line refuses the whole
// paste and adds nothing. The box shows how many lines went through; the count is cleared while it runs.
function ImportOpenings({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const importOpenings = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/openings/import`, body, user),
    onSuccess: (r) => {
      setResult(r);
      setText('');
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setError('Those opening balances could not be pasted: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setResult(null);
    setError('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,period,amount.');
      return;
    }
    once((done) => importOpenings.mutate({ text }, { onSettled: done }));
  };

  return (
    <section data-testid="opening-import-box">
      <form onSubmit={submit}>
        <label>
          Opening balances (name,period,amount){' '}
          <textarea
            data-testid="opening-import"
            rows={4}
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="opening-import-submit" disabled={importOpenings.isPending}>
          Paste openings
        </button>
        {result !== null && (
          <>
            {' '}Added <span data-testid="opening-import-done">{result.added}</span>
          </>
        )}
        {error && (
          <p role="alert" data-testid="opening-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -9.85, Component: ImportOpenings });
