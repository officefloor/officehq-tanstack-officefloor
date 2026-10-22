import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { ratesKey } from './rates';

type ImportResult = { set: number };

/** The rate import box: one grade,rate,from,to line per rate (the from-date and to-date may be absent or blank). The
 *  server checks every line by the rate form's rules first; one failing line refuses the whole import and shows why on
 *  the box, otherwise each line in order sets the grade's rate as the form would (replacing any it had from that same
 *  date), and the box shows how many were set. */
function ImportRates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const importRates = useMutation({
    mutationFn: (body: { user: string; text: string }) =>
      postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/import-rates`, body),
    onSuccess: async (r) => {
      setResult(r);
      setText('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not import: each line is grade,rate,from,to.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    if (text.trim() === '') {
      setError('Paste at least one line: grade,rate,from,to.');
      return;
    }
    if (!once.start()) return;
    importRates.mutate({ user, text });
  };

  return (
    <form data-testid="import-rates-form" onSubmit={submit}>
      <label>
        Import rates (grade,rate,from,to per line){' '}
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
      </button>
      {result !== null && (
        <>
          {' '}Set <span data-testid="import-rates-set">{result.set}</span>
        </>
      )}
      {error && (
        <p data-testid="import-rates-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: 5.1, Component: ImportRates });
