import { useSearchParam } from '../../url/useSearchParam';

export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

const asSortKey = (raw: unknown): SortKey => (SORT_KEYS.includes(raw as SortKey) ? (raw as SortKey) : 'added');

/**
 * How the people list is sorted on this screen, kept in the URL: added (list order) when the screen is first opened,
 * and left as it is by loading a sheet. It changes only what the screen shows, never the list order itself.
 */
export function useSortBy() {
  return useSearchParam('sortBy', asSortKey);
}

/** Names compare as names are compared: trimmed, ignoring case (then as typed). */
export function compareNames(a: string, b: string): number {
  const x = a.trim().toLowerCase();
  const y = b.trim().toLowerCase();
  if (x !== y) return x < y ? -1 : 1;
  return a < b ? -1 : a > b ? 1 : 0;
}

/** Due-totals compare as numbers, lowest first, with n/a last. */
export function compareDue(a: string, b: string): number {
  const x = Number(a);
  const y = Number(b);
  const xa = Number.isNaN(x);
  const ya = Number.isNaN(y);
  if (xa || ya) return xa === ya ? 0 : xa ? 1 : -1;
  return x - y;
}
