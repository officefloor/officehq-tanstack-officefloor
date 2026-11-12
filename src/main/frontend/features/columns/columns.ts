import { asString, useSearchParam } from '../../url/useSearchParam';

/** The people-list columns this screen can switch off, in the order its chooser shows them. */
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

/** The columns this screen has switched off (`hiddenColumns`, comma-separated). It belongs to the screen:
 *  other users' screens, the totals and every other figure are unaffected. */
export function useHiddenColumns(): [Column[], (value: Column[]) => void] {
  const [raw, setRaw] = useSearchParam('hiddenColumns', asString);
  const hidden = raw.split(',').filter((c): c is Column => (COLUMNS as readonly string[]).includes(c));
  return [hidden, (v) => setRaw(COLUMNS.filter((c) => v.includes(c)).join(',') || undefined)];
}

/** Whether this screen shows the column; a column switched off has no cell at all. */
export function useColumnShown(): (column: Column) => boolean {
  const [hidden] = useHiddenColumns();
  return (column) => !hidden.includes(column);
}
