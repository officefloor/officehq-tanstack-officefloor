import { PersonRow } from '../../slots/defs/personRow';
import { setSelectedPerson, useSelectedPeople } from './selection';
import type { Person } from './people';

/** The checkbox that selects the person for a bulk action on the people. */
function PersonSelect({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const chosen = useSelectedPeople(sheetId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={chosen.has(person.id)}
        onChange={(e) => setSelectedPerson(sheetId, person.id, e.target.checked)}
      />
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 1, Component: PersonSelect });
