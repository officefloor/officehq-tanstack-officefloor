import { useCurrentSheet, useCurrentUser } from '../session/session';
import { SORT_KEYS, usePeopleSort } from './people';

// The select that sorts the people list on this screen only; the list's order (positions) is untouched.
export function SortBy() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [sort, setSort] = usePeopleSort();

  if (!user || !sheet) return null;

  return (
    <label>
      Sort by{' '}
      <select data-testid="sort-by" value={sort} onChange={(e) => setSort(e.target.value === 'added' ? undefined : (e.target.value as typeof sort))}>
        {SORT_KEYS.map((k) => (
          <option key={k} value={k}>
            {k}
          </option>
        ))}
      </select>
    </label>
  );
}
