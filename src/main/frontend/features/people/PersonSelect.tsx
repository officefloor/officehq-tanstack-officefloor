import type { Person } from './people';
import { useSelectedPeople } from './selection';

// The checkbox that ticks a person for a bulk action (BulkGrade).
export function PersonSelect({ person }: { person: Person }) {
  const [selected, set] = useSelectedPeople();
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selected.has(person.id)}
        onChange={(e) => set(person.id, e.target.checked)}
      />
    </td>
  );
}
