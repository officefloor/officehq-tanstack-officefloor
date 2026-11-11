import { useClaims } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { NA, heldParts, personDueTotal, useRates } from '../rates/rates';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { liveHolds, usePeople } from './people';
import type { Person } from './people';
import { useCutTotals } from '../pools/pools';

export type SortKey = 'added' | 'name' | 'grade' | 'due-total';
export const SORT_KEYS: SortKey[] = ['added', 'name', 'grade', 'due-total'];

/** How this screen sorts the people list: 'added' (list order) when the screen is first opened; loading a sheet keeps it. */
export function useSortBy(): [SortKey, (key: SortKey) => void] {
  const [raw, set] = useSearchParam('sort', asString);
  const key = (SORT_KEYS as string[]).includes(raw) ? (raw as SortKey) : 'added';
  return [key, (k) => set(k === 'added' ? undefined : k)];
}

// Names are compared trimmed and ignoring case.
const asName = (s: string) => s.trim().toLowerCase();
const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

/**
 * The people in the screen's order: sorting only reorders what is shown, never their positions.
 * By name as names are compared, by grade, or by due-total (as the row shows it, n/a last), each
 * ascending; people with equal keys keep list order.
 */
export function useSorted(sheetId: string, people: Person[]): Person[] {
  const [sortBy] = useSortBy();
  const claims = useClaims(sheetId).data ?? [];
  // Everyone, not only those shown: a part held of a hidden person's claim still counts.
  const everyone = usePeople(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  if (sortBy === 'added') return people;
  const keyed = people.map((p, i) => {
    let key: string | number = 0;
    if (sortBy === 'name') key = asName(p.name);
    else if (sortBy === 'grade') key = p.grade;
    else {
      const total = personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, everyone, claims, rates, today, floor, late), cutOf(p.id));
      const n = Number(total);
      key = total === NA || Number.isNaN(n) ? Infinity : n;
    }
    return { p, i, key };
  });
  keyed.sort((a, b) =>
    (typeof a.key === 'number' && typeof b.key === 'number' ? (a.key < b.key ? -1 : a.key > b.key ? 1 : 0) : cmp(String(a.key), String(b.key))) || a.i - b.i,
  );
  return keyed.map((k) => k.p);
}
