import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useEditingPerson, usePeople } from './people';

// Every person on the sheet, in the order they were added, each with their name and grade and
// the columns features add (slots/defs/personColumns.ts).
export function PeopleList() {
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
          <PersonColumnHeads.Slot />
          <th />
        </tr>
      </thead>
      <tbody>
        {(people.data ?? []).map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name" style={{ whiteSpace: 'pre' }}>
              {p.name}
            </td>
            <td data-testid="person-grade">{p.grade}</td>
            <PersonColumns.Slot person={p} />
            <td>
              <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => setEditing(p.id)}>
                Edit
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
