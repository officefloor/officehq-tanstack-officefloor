import { useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { setAsOf, useAsOf } from '../../api/asOf';
import { SheetPanel } from '../../slots/defs/sheetPanel';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Whether the text is a real date (yyyy-mm-dd). */
function isDate(text: string): boolean {
  if (!DATE.test(text)) return false;
  const d = new Date(`${text}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === text;
}

/**
 * Shows the sheet as of a date, on this user's screen only: every figure, flag, list and line as it stood at the
 * end of that date, the date standing in for the sheet's today. While it is set every change is refused (by the
 * server, with the usual error of the form or action). Clearing it shows the sheet as it is now.
 */
function AsOfView() {
  const asOf = useAsOf();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const [error, setError] = useState('');

  // Every query kept per sheet is read afresh for the new date; none shows what it read for another.
  const show = (date: string) => {
    setAsOf(date);
    void queryClient.resetQueries({ queryKey: ['sheet'] });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = typed.trim();
    if (!isDate(date)) {
      setError('The as-of date must be a date (yyyy-mm-dd).');
      return;
    }
    setError('');
    show(date);
  };

  return (
    <form data-testid="asof-form" onSubmit={submit}>
      As of <strong data-testid="sheet-asof">{asOf}</strong>{' '}
      <input
        data-testid="asof-date"
        placeholder="yyyy-mm-dd"
        value={typed}
        onChange={(e) => {
          setTyped(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="asof-set">
        Show as of
      </button>{' '}
      <button
        type="button"
        data-testid="asof-clear"
        onClick={() => {
          setError('');
          setTyped('');
          show('');
        }}
      >
        Show now
      </button>
      {error && <p data-testid="asof-error">{error}</p>}
    </form>
  );
}

export const contribution = SheetPanel.fill({ order: 0.5, Component: AsOfView });
