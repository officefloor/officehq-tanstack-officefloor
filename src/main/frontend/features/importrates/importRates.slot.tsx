import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { ratesKey } from '../rates/rates';

type Result = { set: number };

// The rate import box: lines of grade,rate,from,to (the from and to may be absent or blank) each set a
// grade's rate, in order, as the rate form would (replacing a rate for the same grade and from-date). The
// server checks every line by the form's rules first: one bad line refuses the whole import and sets
// nothing. The box shows how many were set; the count is cleared while it runs.
function ImportRates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const importRates = useMutation({
    mutationFn: (body: { text: string }) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/import`, body, user),
    onSuccess: (r) => {
      setResult(r);
      setText('');
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onError: () => setError('Those rates could not be imported: check every line.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setResult(null);
    setError('');
    if (text.trim() === '') {
      setError('Paste at least one line of grade,rate,from,to.');
      return;
    }
    once((done) => importRates.mutate({ text }, { onSettled: done }));
  };

  return (
    <section data-testid="import-rates-box">
      <form onSubmit={submit}>
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
        </button>
        {result !== null && (
          <>
            {' '}Set <span data-testid="import-rates-set">{result.set}</span>
          </>
        )}
        {error && (
          <p role="alert" data-testid="import-rates-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -9.9, Component: ImportRates });
