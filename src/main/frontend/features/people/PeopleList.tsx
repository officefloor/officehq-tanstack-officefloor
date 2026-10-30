import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePeople } from './usePeople';

// Every person on the sheet in the order they were added, with their name and grade.
export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
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
        {people.map((p) => (
          <tr key={p.id} data-testid={`person-row-${p.id}`}>
            <td data-testid="person-name">{p.name}</td>
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
