import { asString, useSearchParam } from '../../url/useSearchParam';
import { byNameKey } from '../aliases/aliases';
import { usePersonDueCentsOf } from '../claims/claims';
import type { Person } from './people';

export type SortKey = 'added' | 'name' | 'grade' | 'due-total';
export const SORT_KEYS: SortKey[] = ['added', 'name', 'grade', 'due-total'];

const asSortKey = (raw: unknown): SortKey => {
  const s = asString(raw);
  return (SORT_KEYS as string[]).includes(s) ? (s as SortKey) : 'added';
};

/** How this user's screen orders the people list: list order ("added") until changed, kept in the URL. It changes
 *  only what the screen shows — positions, the rollup lines and every action in list order are unaffected. */
export const useSortBy = () => useSearchParam('sort', asSortKey);

/** The people (in list order) as the screen shows them: sorted by the key, equal keys keeping list order; by name as
 *  names are compared, by grade A to C, by due-total smallest first with n/a last. */
export function useSorted(sheet: string, people: Person[]): Person[] {
  const [sortBy] = useSortBy();
  const dueOf = usePersonDueCentsOf(sheet);
  if (sortBy === 'name') return [...people].sort((a, b) => byNameKey(a.name, b.name));
  if (sortBy === 'grade') return [...people].sort((a, b) => (a.grade < b.grade ? -1 : a.grade > b.grade ? 1 : 0));
  if (sortBy === 'due-total') {
    const due = new Map(people.map((p) => [p.id, dueOf(p)]));
    return [...people].sort((a, b) => {
      const x = due.get(a.id);
      const y = due.get(b.id);
      if (x === undefined || y === undefined) return x === y ? 0 : x === undefined ? 1 : -1;
      return x < y ? -1 : x > y ? 1 : 0;
    });
  }
  return people;
}
