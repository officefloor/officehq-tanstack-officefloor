import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from './usePersonError';
import { useSelectedPeople } from './useSelectedPeople';

/** The checkbox that ticks a person for a bulk action; ticking it is the next action on the row, so it clears the row's refusal. */
function PersonSelect({ personId }: { sheetId: string; personId: number }) {
  const [selected, setSelected] = useSelectedPeople();
  const [, setError] = usePersonError(personId);
  const checked = selected.includes(personId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={checked}
        onChange={(e) => {
          setError('');
          setSelected(e.target.checked ? [...selected, personId] : selected.filter((id) => id !== personId));
        }}
      />
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 0, Component: () => <th /> }),
  PersonCells.fill({ order: 0, Component: PersonSelect }),
];
