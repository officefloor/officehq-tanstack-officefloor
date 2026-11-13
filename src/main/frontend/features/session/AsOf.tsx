import { useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { sheetKey, useCurrentSheet, useCurrentUser } from './session';

// The date this screen shows the sheet as of: every figure, flag, list and line as it stood at the
// end of that date, with the date standing in for the sheet's today. It belongs to this screen only
// (the `asof` search param); while it is set every change is refused (api/http).
export function AsOf() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [asOf] = useSearchParam('asof', asString);
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const show = async (value: string) => {
    await navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        if (value) next.asof = value;
        else delete next.asof;
        return next;
      },
      replace: true,
    });
    // Everything kept per sheet is re-read as of the new date.
    await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = date.trim();
    setError('');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed) || Number.isNaN(Date.parse(trimmed))) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    void show(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <span>
        As of: <strong data-testid="sheet-asof">{asOf}</strong>{' '}
      </span>
      <input
        data-testid="asof-date"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="asof-set">
        Set as of
      </button>
      <button
        type="button"
        data-testid="asof-clear"
        onClick={() => {
          setError('');
          void show('');
        }}
      >
        Clear as of
      </button>
      {error && <span data-testid="asof-error">{error}</span>}
    </form>
  );
}
