import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { usePersonDue } from '../rates/due.slot';
import { type Person, shownName, useEditingPerson, usePeople, useSelectedPeople, useShowRetired } from './people';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useColumnShown } from '../columns/columns';
import { useRoles } from '../roles/roles';
import { useCrews } from '../crews/crews';

/** Whether this screen's user may see the person's row: a restricted user sees only the person with their
 *  name and the people of the crews that person leads or belongs to (nothing until roles and crews are read);
 *  anyone else sees every row. Only the rows are restricted — every total is still over the whole sheet. */
function useRowVisible(sheet: string): (p: Person) => boolean {
  const [user] = useCurrentUser();
  const roles = useRoles(sheet);
  const crews = useCrews(sheet);
  const people = usePeople(sheet);
  if (!roles.data) return () => false;
  if (!roles.data.some((r) => r.user === user && r.role === 'restricted')) return () => true;
  if (!crews.data || !people.data) return () => false;
  const self = people.data.find((p) => p.name === user);
  if (!self) return () => false;
  const ids = new Set<number>([self.id]);
  for (const c of crews.data) {
    const members = [c.leadId, ...c.members.map((m) => m.personId)];
    if (members.includes(self.id)) members.forEach((id) => ids.add(id));
  }
  return (p) => ids.has(p.id);
}

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
  const shown = useColumnShown();
  const visible = useRowVisible(sheet);

  return (
    <>
    <SortBy />
    <table data-testid="people">
      <thead>
        <tr>
          <th />
          <th>Name</th>
          <th>Grade</th>
          {shown('terms') && <th>Terms</th>}
          {shown('contact') && <th>Contact</th>}
          <th>Tags</th>
          <th>Snapshots</th>
          <th>Clashing</th>
          <PersonColumnHeads.Slot />
          <th />
        </tr>
      </thead>
      <tbody>
        {sorted(people.data ?? [], sortBy, dueOf).filter((p) => (showRetired || !p.retired) && visible(p)).map((p) => (
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
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>{shownName(p)}</td>
            <td data-testid="person-grade">{p.grade}</td>
            {shown('terms') && <td data-testid="person-terms">{p.terms ?? ''}</td>}
            {shown('contact') && <td data-testid="person-contact">{p.contact ?? ''}</td>}
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
