import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useEditingPerson, usePeople } from './people';

// Every person on the sheet, in the order they were added, each with their name and grade.
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
