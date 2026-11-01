import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The date this screen shows the sheet as of (search param `asof`, so it is this user's screen only). While it is
// set every request carries it (api/http.ts): every read answers as the sheet stood at the end of that date, with
// the date for its today, and every change is refused.
function AsOf() {
  const queryClient = useQueryClient();
  const [asof, setAsof] = useSearchParam('asof', asString);
  const [draft, setDraft] = useState('');
  const shown = useRef(asof);

  // Once the URL carries the new date, everything on screen is read again for it.
  useEffect(() => {
    if (shown.current === asof) return;
    shown.current = asof;
    void queryClient.resetQueries();
  }, [asof, queryClient]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const date = draft.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(date) && !Number.isNaN(Date.parse(date))) setAsof(date);
  };

  return (
    <form onSubmit={submit}>
      As of: <strong data-testid="sheet-asof">{asof}</strong>{' '}
      <input
        data-testid="asof-date"
        type="text"
        placeholder="yyyy-mm-dd"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
      />
      <button type="submit" data-testid="asof-set">
        Show as of
      </button>
      <button type="button" data-testid="asof-clear" onClick={() => setAsof(undefined)}>
        Show now
      </button>
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 1, Component: AsOf });
