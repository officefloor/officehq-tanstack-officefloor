import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useClashing } from '../people/usePeople';

/** Whether the person's name or an alias is already a name or alias on another sheet of the sheet's tree (yes/no).
 *  Such a person is kept as they are, but the person form refuses every edit until it renames them clear. */
function PersonClashing({ sheetId, personId }: { sheetId: string; personId: number }) {
  const clashing = useClashing(sheetId);
  return <td data-testid="person-clashing">{(clashing.data ?? []).includes(personId) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonColumnHead.fill({ order: 3.5, Component: () => <th>Clashing</th> }),
  PersonCells.fill({ order: 3.5, Component: PersonClashing }),
];
