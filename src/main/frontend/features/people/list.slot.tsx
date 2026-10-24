import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople, useEditingPerson } from './people';

// The sheet's people in the order they were added, each with their name, grade and an edit button;
// features add their own columns through PeopleColumns / PersonCells.
function PeopleList() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setEditing] = useEditingPerson();
  const people = usePeople();
  if (!user || !sheet) return null;
  return (
    <table data-testid="people">
      <thead>
        <tr>
          <th>Name</th>
          <th>Grade</th>
          <th />
          <PeopleColumns.Slot />
        </tr>
      </thead>
      <tbody>
        {(people.data ?? []).map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>{p.name}</td>
            <td data-testid="person-grade">{p.grade}</td>
            <td>
              <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(String(p.id))}>
                Edit
              </button>
            </td>
            <PersonCells.Slot person={p} />
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = HomeMain.fill({ order: 20, Component: PeopleList });
