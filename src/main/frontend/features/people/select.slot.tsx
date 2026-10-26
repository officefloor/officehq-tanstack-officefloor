import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { useSelectedPeople } from './useSelectedPeople';

/** The checkbox that ticks a person for a bulk action. */
function PersonSelect({ personId }: { sheetId: string; personId: number }) {
  const [selected, setSelected] = useSelectedPeople();
  const checked = selected.includes(personId);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={checked}
        onChange={(e) =>
          setSelected(e.target.checked ? [...selected, personId] : selected.filter((id) => id !== personId))
        }
      />
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 0, Component: () => <th /> }),
  PersonCells.fill({ order: 0, Component: PersonSelect }),
];
