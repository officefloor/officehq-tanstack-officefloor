import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';
import { PersonForm } from './PersonForm';
import { useEditingPerson } from './useEditingPerson';
import { useShowRetired } from './useShowRetired';
import { compareDue, compareNames, SORT_KEYS, useSortBy } from './useSortBy';
import type { SortKey } from './useSortBy';
import { usePeopleDueTotals } from '../rates/due.slot';
import { ColumnChooser, ColumnShown } from './useColumns';

function People({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId));
  const [, setEditingId] = useEditingPerson();
  const [showRetired, setShowRetired] = useShowRetired();
  // Retired people have no row at all unless "show retired" is on; their claims still count in every total.
  const [sortBy, setSortBy] = useSortBy();
  const dues = usePeopleDueTotals(sheetId);
  // Sorting changes only the order shown; equal keys keep list order (the sort is stable).
  const byKey: Record<SortKey, ((a: Person, b: Person) => number) | null> = {
    added: null,
    name: (a, b) => compareNames(a.name, b.name),
    grade: (a, b) => a.grade.localeCompare(b.grade),
    'due-total': (a, b) => compareDue(dues.get(a.id) ?? 'n/a', dues.get(b.id) ?? 'n/a'),
  };
  const compare = byKey[sortBy];
  const filtered = (people.data ?? []).filter((p) => showRetired || !p.retiredFrom);
  const listed = compare ? [...filtered].sort(compare) : filtered;

  return (
    <section data-testid="people">
      <h3>People</h3>
      <PersonForm sheetId={sheetId} />
      <label>
        <input
          type="checkbox"
          data-testid="show-retired"
          checked={showRetired}
          onChange={(e) => setShowRetired(e.target.checked || undefined)}
        />{' '}
        Show retired
      </label>{' '}
      <label>
        Sort by{' '}
        <select data-testid="sort-by" value={sortBy} onChange={(e) => setSortBy(e.target.value === 'added' ? undefined : (e.target.value as SortKey))}>
          {SORT_KEYS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      <ColumnChooser />
      {people.isError && <p data-testid="people-load-error">Could not load the people.</p>}
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <ColumnShown column="terms"><th>Terms</th></ColumnShown>
            <ColumnShown column="contact"><th>Contact</th></ColumnShown>
            <th>Tags</th>
            <PersonHeaders.Slot sheetId={sheetId} />
            <th />
          </tr>
        </thead>
        <tbody>
          {listed.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <ColumnShown column="terms"><td data-testid="person-terms">{p.terms ?? ''}</td></ColumnShown>
              <ColumnShown column="contact"><td data-testid="person-contact">{p.contact ?? ''}</td></ColumnShown>
              <td data-testid="person-tags">{p.tags.join(', ')}</td>
              <PersonCells.Slot sheetId={sheetId} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditingId(p.id)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// Keyed by sheet, so a typed name or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 10,
  Component: ({ sheetId }) => <People key={sheetId} sheetId={sheetId} />,
});
