import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { peopleQuery } from './peopleQuery';

/** The sheet the person was transferred from (see `Person.origin`); empty for anyone not transferred in. */
function PersonOrigin({ sheetId, personId }: { sheetId: string; personId: number }) {
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  return <td data-testid="person-origin">{person?.origin ?? ''}</td>;
}

export const contributions = [
  PersonHeaders.fill({ order: 9.6, Component: () => <th>From</th> }),
  PersonCells.fill({ order: 9.6, Component: PersonOrigin }),
];
