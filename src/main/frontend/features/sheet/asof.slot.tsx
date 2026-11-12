import { useState } from 'react';
import { AppHeader } from '../../slots/defs/appHeader';
import { useAsOf, useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheet } from './sheet';

// The screen's as-of date: while one is set, every cell shows the sheet as it stood at the end of that
// date (the date standing in for the sheet's today), and every change is refused. Only this screen's.
function SheetAsOf() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setAsOf] = useAsOf();
  const loaded = useSheet(sheet);
  const [input, setInput] = useState('');

  if (!user || sheet === '') return null;
  return (
    <form
      data-testid="asof-form"
      onSubmit={(e) => {
        e.preventDefault();
        const date = input.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) setAsOf(date);
      }}
    >
      As of <strong data-testid="sheet-asof">{loaded.data?.asOf ?? ''}</strong>{' '}
      <input data-testid="asof-date" type="date" value={input} onChange={(e) => setInput(e.target.value)} />
      <button type="submit" data-testid="asof-set">
        Show as of
      </button>
      <button type="button" data-testid="asof-clear" onClick={() => setAsOf(undefined)}>
        Show as now
      </button>
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 25, Component: SheetAsOf });
