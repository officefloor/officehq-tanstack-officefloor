import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet } from '../../session/session';
import { usePersonDue } from '../rates/due.slot';
import { type Person, useEditingPerson, usePeople, useSelectedPeople, useShowRetired } from './people';
import { asString, useSearchParam } from '../../url/useSearchParam';

const SORTS = ['added', 'name', 'grade', 'due-total'] as const;
type Sort = (typeof SORTS)[number];

/** What this screen sorts the people list by (`sortBy`; 'added' — list order — when none is set). It belongs
 *  to the screen: other users' screens, the positions and everything that works in list order are unaffected. */
const useSortBy = (): [Sort, (value: Sort) => void] => {
  const [raw, setRaw] = useSearchParam('sortBy', asString);
  return [(SORTS as readonly string[]).includes(raw) ? (raw as Sort) : 'added', (v) => setRaw(v === 'added' ? undefined : v)];
};

/** Names compare as the store compares them: trimmed and lower-cased, then as written. */
const nameKey = (name: string) => name.trim().toLowerCase();
const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

/** The people sorted by the key; equal keys (and 'added') keep list order. A due-total of n/a sorts last. */
function sorted(people: Person[], by: Sort, dueOf: (p: Person) => string): Person[] {
  if (by === 'added') return people;
  const compare = (a: Person, b: Person): number => {
    if (by === 'name') return cmp(nameKey(a.name), nameKey(b.name)) || cmp(a.name, b.name);
    if (by === 'grade') return cmp(a.grade, b.grade);
    const da = Number(dueOf(a));
    const db = Number(dueOf(b));
    const na = Number.isNaN(da);
    const nb = Number.isNaN(db);
    if (na || nb) return Number(na) - Number(nb);
    return da - db;
  };
  return people.map((p, i) => ({ p, i })).sort((x, y) => compare(x.p, y.p) || x.i - y.i).map((x) => x.p);
}

function SortBy() {
  const [by, setBy] = useSortBy();
  return (
    <label>
      Sort by{' '}
      <select data-testid="sort-by" value={by} onChange={(e) => setBy(e.target.value as Sort)}>
        {SORTS.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
    </label>
  );
}

// The sheet's people in list order (by their positions, see move.slot.tsx); retired people only while "show retired" is on.
function PeopleList() {
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople(sheet);
  const [showRetired] = useShowRetired();
  const [selected, setSelected] = useSelectedPeople();
  const [sortBy] = useSortBy();
  const dueOf = usePersonDue(sheet);

  return (
    <>
    <SortBy />
    <table data-testid="people">
      <thead>
        <tr>
          <th />
          <th>Name</th>
          <th>Grade</th>
          <th>Terms</th>
          <th>Contact</th>
          <th>Tags</th>
          <th>Snapshots</th>
          <th>Clashing</th>
          <PersonColumnHeads.Slot />
          <th />
        </tr>
      </thead>
      <tbody>
        {sorted(people.data ?? [], sortBy, dueOf).filter((p) => showRetired || !p.retired).map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td>
              <input
                type="checkbox"
                data-testid="person-select"
                aria-label="Select person"
                checked={selected.includes(p.id)}
                onChange={(e) => setSelected(e.target.checked ? [...selected, p.id] : selected.filter((id) => id !== p.id))}
              />
            </td>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>{p.name}</td>
            <td data-testid="person-grade">{p.grade}</td>
            <td data-testid="person-terms">{p.terms ?? ''}</td>
            <td data-testid="person-contact">{p.contact ?? ''}</td>
            <td data-testid="person-tags">{p.tags.join(', ')}</td>
            <td>
              {p.snapshots.map((n, i) => (
                <div key={i} data-testid="person-snapshot-line">
                  {n.day} {n.dueTotal}
                </div>
              ))}
            </td>
            <td data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</td>
            <PersonColumns.Slot person={p} />
            <td>
              <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(String(p.id))}>
                Edit
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
    </>
  );
}

export const contribution = HomeMain.fill({ order: 20, Component: PeopleList });
