import { asString, useSearchParam } from '../../url/useSearchParam';
import { useDueTotals } from '../rates/rates.slot';
import type { Person } from '../people/people.slot';

/** The keys the people list can be sorted by on the screen; `added` is list order. */
export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

/** How this user's screen sorts the people list (search param `sortBy`): list order when unset. */
export function useSortBy(): [SortKey, (value: SortKey | undefined) => void] {
  const [raw, set] = useSearchParam('sortBy', asString);
  const key = (SORT_KEYS as readonly string[]).includes(raw) ? (raw as SortKey) : 'added';
  return [key, (value) => set(value === 'added' ? undefined : value)];
}

/** Names compared as names are compared: ignoring case, then as typed. */
function compareNames(a: string, b: string): number {
  const la = a.toLowerCase();
  const lb = b.toLowerCase();
  if (la !== lb) return la < lb ? -1 : 1;
  return a < b ? -1 : a > b ? 1 : 0;
}

/** The people as this user's screen shows them: sorted by the chosen key, equal keys in list order (the sort is
 *  stable), a due-total of n/a last. Only what the screen shows changes; list order itself is untouched. */
export function useSortedPeople(sheetId: string, people: Person[]): Person[] {
  const [sortBy] = useSortBy();
  const totals = useDueTotals(sheetId);
  if (sortBy === 'added') return people;
  const sorted = [...people];
  if (sortBy === 'name') sorted.sort((a, b) => compareNames(a.name, b.name));
  if (sortBy === 'grade') sorted.sort((a, b) => (a.grade < b.grade ? -1 : a.grade > b.grade ? 1 : 0));
  if (sortBy === 'due-total') {
    sorted.sort((a, b) => {
      const ta = totals.get(a.id) ?? null;
      const tb = totals.get(b.id) ?? null;
      if (ta === null || tb === null) return ta === tb ? 0 : ta === null ? 1 : -1;
      return ta < tb ? -1 : ta > tb ? 1 : 0;
    });
  }
  return sorted;
}
