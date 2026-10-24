import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { PEOPLE_SORTS, useSortPeople, type PeopleSort } from './people';

// The sort select above the people list: added (list order), name, grade in force on the sheet's today, or
// due-total (n/a last). Only this screen's rows move; list order is untouched.
function SortPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [sort, setSort] = useSortPeople();
  if (!user || !sheet) return null;
  return (
    <label>
      Sort by{' '}
      <select data-testid="sort-by" value={sort}
        onChange={(e) => setSort(e.target.value === 'added' ? undefined : (e.target.value as PeopleSort))}>
        {PEOPLE_SORTS.map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
    </label>
  );
}

export const contribution = HomeMain.fill({ order: 18.5, Component: SortPeople });
