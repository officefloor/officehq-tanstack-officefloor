import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet } from '../../session/session';
import { useEditingPerson, usePeople, useSelectedPeople, useShowRetired } from './people';

// The sheet's people in list order (by their positions, see move.slot.tsx); retired people only while "show retired" is on.
function PeopleList() {
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople(sheet);
  const [showRetired] = useShowRetired();
  const [selected, setSelected] = useSelectedPeople();

  return (
    <table data-testid="people">
      <thead>
        <tr>
          <th />
          <th>Name</th>
          <th>Grade</th>
          <th>Terms</th>
          <th>Contact</th>
          <th>Tags</th>
          <th>Clashing</th>
          <PersonColumnHeads.Slot />
          <th />
        </tr>
      </thead>
      <tbody>
        {(people.data ?? []).filter((p) => showRetired || !p.retired).map((p) => (
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
  );
}

export const contribution = HomeMain.fill({ order: 20, Component: PeopleList });
