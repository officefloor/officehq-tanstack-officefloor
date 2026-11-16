import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { SheetPage } from '../../slots/defs/sheetPage';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The date this screen shows the sheet as of (yyyy-MM-dd), or '' for the sheet as it is now. It belongs to this
 *  user's screen (the `asof` search param), so loading another sheet keeps it, and no other user sees it. While it
 *  is set every read asks for the sheet as it stood at the end of that date and every change is refused (see
 *  api/http). */
export const useAsOf = () => useSearchParam('asof', asString);

/** The as-of date: an entry, set and clear, and the date shown (nothing while the sheet is shown as it is now).
 *  Setting or clearing it re-reads everything on the screen. */
function AsOf() {
  const [asOf] = useAsOf();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState('');
  const show = async (date: string) => {
    await navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        if (date === '') delete next.asof;
        else next.asof = date;
        return next;
      },
      replace: true,
    });
    await queryClient.resetQueries();
  };
  return (
    <form
      data-testid="asof-form"
      onSubmit={(e) => {
        e.preventDefault();
        const date = typed.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) void show(date);
      }}
    >
      As of <span data-testid="sheet-asof">{asOf}</span>{' '}
      <input type="date" data-testid="asof-date" value={typed} onChange={(e) => setTyped(e.target.value)} />
      <button type="submit" data-testid="asof-set">
        Show as of
      </button>{' '}
      <button type="button" data-testid="asof-clear" onClick={() => void show('')}>
        Clear
      </button>
    </form>
  );
}

export const contribution = SheetPage.fill({ order: 1, Component: AsOf });
