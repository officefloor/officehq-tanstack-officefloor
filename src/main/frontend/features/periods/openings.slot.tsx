import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { periodsKey } from './periodsQuery';

/**
 * The opening balance box: lines of name,period,amount (a person by name or alias, a period on the sheet, an amount
 * with two decimals, 0.00 or more). The server checks every line first and adds nothing when any is refused; otherwise
 * each adds its amount to the person's carried figure for the period. Shows how many lines went through.
 */
function ImportOpenings({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState('');
  const once = useSubmitOnce();
  const importOpenings = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/openings/import`, body, user),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setText('');
      setDone(String(res.set));
    },
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The opening balances could not be imported: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setDone('');
    if (text.trim() === '') {
      setError('Paste at least one line of name,period,amount.');
      return;
    }
    once(() => importOpenings.mutateAsync({ text }));
  };

  return (
    <form data-testid="opening-import-box" onSubmit={submit}>
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
        Import opening balances
      </button>{' '}
      Imported: <span data-testid="opening-import-done">{done}</span>
      {error && <p data-testid="opening-error">{error}</p>}
    </form>
  );
}

// Below the periods; keyed by sheet, so pasted text, a count or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 7.1,
  Component: ({ sheetId }) => <ImportOpenings key={sheetId} sheetId={sheetId} />,
});
