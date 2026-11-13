import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

type ImportRatesResult = { set: number };

// The rate import box: lines of grade,rate,from,to (the from- and to-date may be left off or blank), each set as the
// rate form sets it. The server checks every line with the rate form's rules first, so one refused line sets nothing.
export function ImportRates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [set, setSet] = useState<number | undefined>(undefined);
  const once = useOneAtATime();
  const queryClient = useQueryClient();

  const importRates = useMutation({
    mutationFn: (pasted: string) =>
      postJson<ImportRatesResult>(
        `/api/sheets/${encodeURIComponent(sheet)}/import-rates?user=${encodeURIComponent(user)}`,
        { text: pasted },
      ),
    onSuccess: async (result) => {
      setText('');
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
      setSet(result.set);
    },
    onError: () => setError('Nothing was set: every line must be a rate the rate form takes, as grade,rate,from,to.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSet(undefined);
    if (!text.trim()) {
      setError('Paste at least one line of grade,rate,from,to.');
      return;
    }
    once(() => importRates.mutateAsync(text));
  };

  return (
    <form onSubmit={submit} data-testid="import-rates-form">
      <textarea
        data-testid="import-rates"
        placeholder="grade,rate,from,to — one rate per line"
        rows={4}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>
      {set !== undefined && (
        <>
          {' '}
          Set <span data-testid="import-rates-set">{set}</span>
        </>
      )}
      {error && <span data-testid="import-rates-error">{error}</span>}
    </form>
  );
}
