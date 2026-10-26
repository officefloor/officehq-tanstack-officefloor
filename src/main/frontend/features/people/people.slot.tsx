import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from './peopleQuery';
import { PersonForm } from './PersonForm';
import { useEditingPerson } from './useEditingPerson';

function People({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId));
  const [, setEditingId] = useEditingPerson();

  return (
    <section data-testid="people">
      <h3>People</h3>
      <PersonForm sheetId={sheetId} />
      {people.isError && <p data-testid="people-load-error">Could not load the people.</p>}
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonHeaders.Slot sheetId={sheetId} />
            <th />
          </tr>
        </thead>
        <tbody>
          {(people.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
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
