import { useClaims } from '../claims/claims';
import { capped, personDueSum, toCents, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { gradeOn, type Person } from './people';

export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

/** What this user's screen sorts the people list by ('added', list order, unless chosen). It belongs to the
 *  screen, not the sheet: other users are unaffected, and loading a sheet leaves it as it is. */
export function useSortBy(): [SortKey, (key: SortKey) => void] {
  const [raw, set] = useSearchParam('sortBy', asString);
  const key = (SORT_KEYS as readonly string[]).includes(raw) ? (raw as SortKey) : 'added';
  return [key, (k) => set(k === 'added' ? undefined : k)];
}

/** A name as names are compared: without regard to case, ignoring surrounding spaces. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The people (given in list order) as the screen shows them, sorted by the screen's key: by name as names are
 *  compared, by the grade in force, or by due-total as shown (n/a last); equal keys keep list order. Only what the
 *  screen shows changes: positions and everything done in list order are unaffected. */
export function useSortedPeople(sheetId: string, people: Person[]): Person[] {
  const [sortBy] = useSortBy();
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  if (sortBy === 'added') return people;
  const compare = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
  const keyed = people.map((p, i) => {
    if (sortBy === 'name') return { p, i, key: nameKey(p.name) };
    if (sortBy === 'grade') return { p, i, key: (today !== '' && gradeOn(p, today)) || p.grade };
    const due = capped(personDueSum(p, claims, rates, today, floor, late, people), cap);
    return { p, i, key: due, cents: due === 'n/a' ? null : toCents(due) };
  });
  return keyed
    .sort((x, y) => {
      if (sortBy === 'due-total') {
        const a = 'cents' in x ? x.cents : null;
        const b = 'cents' in y ? y.cents : null;
        const c = a === null ? (b === null ? 0 : 1) : b === null ? -1 : a < b ? -1 : a > b ? 1 : 0;
        return c || x.i - y.i;
      }
      return compare(x.key, y.key) || x.i - y.i;
    })
    .map((k) => k.p);
}
