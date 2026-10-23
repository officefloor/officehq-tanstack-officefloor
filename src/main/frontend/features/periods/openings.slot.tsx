import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { periodsKey } from './periods';

type ImportResult = { imported: number };

/** The opening-balance paste box: one name,period,amount line per opening (a person on the sheet by name or alias, a
 *  period on the sheet, an amount of two decimals, 0.00 or more). The server checks every line first; one failing line
 *  refuses the whole paste and shows why on the box, otherwise each line adds its amount to the person's carried figure
 *  for the period, and the box shows how many lines went through. */
function ImportOpenings({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const importOpenings = useMutation({
    mutationFn: (body: { user: string; text: string }) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/import-openings`, body),
    onSuccess: async (r) => {
      setResult(r);
      setText('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not paste: each line is name,period,amount.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    if (text.trim() === '') {
      setError('Paste at least one line: name,period,amount.');
      return;
    }
    if (!once.start()) return;
    importOpenings.mutate({ user, text });
  };

  return (
    <form data-testid="opening-import-form" onSubmit={submit}>
      <label>
        Opening balances (name,period,amount per line){' '}
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
          {' '}Lines <span data-testid="opening-import-done">{result.imported}</span>
        </>
      )}
      {error && (
        <p data-testid="opening-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 6.1, Component: ImportOpenings });
