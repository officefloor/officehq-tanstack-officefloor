import { useSheetTotals } from '../claims/useSheetTotals';
import { useSheetToday } from '../sheet/useSheet';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { gradeOn, type Person } from './usePeople';

export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export type SortKey = (typeof SORT_KEYS)[number];

const asSortKey = (raw: unknown): SortKey => {
  const s = asString(raw);
  return (SORT_KEYS as readonly string[]).includes(s) ? (s as SortKey) : 'added';
};

/** How this user's screen orders the people list. "added" (list order) when the screen is first
 *  opened; kept in the URL, so loading a sheet leaves it as it is. Only the screen is reordered:
 *  positions, rollups and every action in list order are unaffected. */
export function useSortBy() {
  return useSearchParam('sort', asSortKey);
}

/** The form a name is compared in: trimmed and lower-cased. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The people, in list order, sorted for the screen by the chosen key. Equal keys keep list order;
 *  by due-total (ascending) a person whose due-total is n/a comes last. */
export function useSortedPeople(sheet: string, people: Person[]): Person[] {
  const [key] = useSortBy();
  const today = useSheetToday(sheet);
  const { perPerson } = useSheetTotals(sheet);
  if (key === 'added') return people;
  const dueOf = new Map(perPerson.map((r) => [r.person.id, r.due]));
  const cmp = (a: Person, b: Person): number => {
    if (key === 'name') {
      const x = nameKey(a.name);
      const y = nameKey(b.name);
      return x < y ? -1 : x > y ? 1 : 0;
    }
    if (key === 'grade') {
      const x = gradeOn(a, today);
      const y = gradeOn(b, today);
      return x < y ? -1 : x > y ? 1 : 0;
    }
    const x = dueOf.get(a.id) ?? null;
    const y = dueOf.get(b.id) ?? null;
    if (x === null || y === null) return x === y ? 0 : x === null ? 1 : -1;
    return x < y ? -1 : x > y ? 1 : 0;
  };
  // Array.prototype.sort is stable, so equal keys keep list order.
  return [...people].sort(cmp);
}

/** The "sort by" select above the people list. */
export function SortBy() {
  const [key, setKey] = useSortBy();
  return (
    <label>
      Sort by{' '}
      <select
        data-testid="sort-by"
        value={key}
        onChange={(e) => setKey(e.target.value === 'added' ? undefined : (e.target.value as SortKey))}
      >
        {SORT_KEYS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>
    </label>
  );
}
