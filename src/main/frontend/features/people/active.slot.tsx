import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsQuery, isActive } from '../claims/claimsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { peopleQuery } from './peopleQuery';

/** Whether the person is active (see `isActive`): yes or no. */
function PersonActive({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  return <td data-testid="person-active">{isActive(personId, claims, people, today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonHeaders.fill({ order: 11, Component: () => <th>Active</th> }),
  PersonCells.fill({ order: 11, Component: PersonActive }),
];
