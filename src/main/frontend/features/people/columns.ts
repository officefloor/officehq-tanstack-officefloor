import { useNavigate, useSearch } from '@tanstack/react-router';

/** The people list's columns this user's screen can switch off, each named as its cell is. */
export const COLUMNS = [
  'claimed',
  'due-total',
  'excess',
  'statement-count',
  'stated-total',
  'statement-date',
  'overdue-count',
  'overdue-total',
  'patrons',
  'backing',
  'hold',
  'retired',
  'terms',
  'contact',
  'aliases',
] as const;

export type Column = (typeof COLUMNS)[number];

/** The columns switched off on this user's screen: every column is shown when the screen is first opened,
 *  and loading a sheet leaves the choice as it is. Only the screen changes — no figure does. */
export function useHiddenColumns(): [Set<string>, (column: Column, shown: boolean) => void] {
  const search = useSearch({ strict: false }) as Record<string, unknown>;
  const navigate = useNavigate();
  const set = (column: Column, shown: boolean) => {
    void navigate({
      to: '.',
      // Worked out from the URL as it is when the change lands, so switches made in quick succession all count.
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        const off = hiddenIn(prev.hideColumns);
        if (shown) off.delete(column);
        else off.add(column);
        const value = COLUMNS.filter((c) => off.has(c)).join(',');
        if (value === '') delete next.hideColumns;
        else next.hideColumns = value;
        return next;
      },
      replace: true,
    });
  };
  return [hiddenIn(search.hideColumns), set];
}

function hiddenIn(raw: unknown): Set<string> {
  return new Set((typeof raw === 'string' ? raw : '').split(',').filter((c) => c !== ''));
}

/** Whether this user's screen shows the column. */
export function useColumnShown(column: Column): boolean {
  return !useHiddenColumns()[0].has(column);
}
