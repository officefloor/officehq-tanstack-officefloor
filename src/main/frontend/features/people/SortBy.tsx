import { asString, useSearchParam } from '../../url/useSearchParam';
import type { Person } from './api';

export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

/** How this screen orders the people list. It lives in the URL (`sort`), so it belongs to each
 *  user's own screen: `added` (list order) when the screen is first opened, and left as it is by
 *  loading a sheet. */
export const useSortBy = () => useSearchParam('sort', (raw): SortKey =>
  (SORT_KEYS as readonly unknown[]).includes(raw) ? (raw as SortKey) : 'added');

/** Names as they are compared: trimmed and lower-cased. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The people in the order the screen shows them. Only the screen changes: the list order (and so
 *  positions, rollups and every action in list order) is left as it is. `due-total` puts n/a (null)
 *  last; equal keys keep list order. */
export function sortPeople<P extends Person>(people: P[], key: SortKey, dueOf: (p: P) => string | null): P[] {
  if (key === 'added') return people;
  const cmp: (a: P, b: P) => number =
    key === 'name' ? (a, b) => { const x = nameKey(a.name), y = nameKey(b.name); return x < y ? -1 : x > y ? 1 : 0; }
    : key === 'grade' ? (a, b) => (a.grade < b.grade ? -1 : a.grade > b.grade ? 1 : 0)
    : (a, b) => {
      const x = dueOf(a), y = dueOf(b);
      if (x === null || y === null) return (x === null ? 1 : 0) - (y === null ? 1 : 0);
      return Number(x) - Number(y);
    };
  // Array.prototype.sort is stable, so equal keys keep list order.
  return [...people].sort(cmp);
}

/** The select that sorts the people list on this screen. */
export function SortBy() {
  const [key, setKey] = useSortBy();
  return (
    <label>
      Sort by{' '}
      <select data-testid="sort-by" value={key} onChange={(e) => setKey(e.target.value === 'added' ? undefined : (e.target.value as SortKey))}>
        {SORT_KEYS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>
    </label>
  );
}
