import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { ratesKey } from './ratesQuery';

/**
 * The rate import box: lines of grade,rate,from,to, in order. The server checks every line by the rate form's rules
 * first and sets nothing when any is refused; otherwise each is set as the rate form sets it. Shows how many were set.
 */
function ImportRates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [set, setSet] = useState('');
  const once = useSubmitOnce();
  const importRates = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/import`, body, user),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setText('');
      setSet(String(res.set));
    },
    onError: (e) =>
      setError(e instanceof RefusedError ? e.message : 'The rates could not be imported: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSet('');
    if (text.trim() === '') {
      setError('Paste at least one line of grade,rate,from,to.');
      return;
    }
    once(() => importRates.mutateAsync({ text }));
  };

  return (
    <form data-testid="import-rates-box" onSubmit={submit}>
      <label>
        Import rates (grade,rate,from,to){' '}
        <textarea
          data-testid="import-rates"
          rows={4}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>{' '}
      Set: <span data-testid="import-rates-set">{set}</span>
      {error && <p data-testid="import-rates-error">{error}</p>}
    </form>
  );
}

// Below the rates; keyed by sheet, so pasted text, a count or an error never carries over.
export const contribution = SheetPanel.fill({
  order: 5.5,
  Component: ({ sheetId }) => <ImportRates key={sheetId} sheetId={sheetId} />,
});
