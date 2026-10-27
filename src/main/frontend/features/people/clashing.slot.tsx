import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { peopleQuery } from './peopleQuery';

/** Whether the person's name or an alias is also used elsewhere on the sheet's tree (see `Person.clashing`): yes or no. */
function PersonClashing({ sheetId, personId }: { sheetId: string; personId: number }) {
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  return <td data-testid="person-clashing">{person?.clashing ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonHeaders.fill({ order: 9.5, Component: () => <th>Clashing</th> }),
  PersonCells.fill({ order: 9.5, Component: PersonClashing }),
];
