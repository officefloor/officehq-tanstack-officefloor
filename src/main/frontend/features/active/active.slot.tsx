import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import type { Person } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { isActive } from './active';

/** Whether a person is active: yes when they have an OPEN or PENDING claim that counts dated within
 *  the 30 days up to and including the sheet's today. */
function PersonActive({ sheet, person }: { user: string; sheet: string; person: Person }) {
  const { data: all = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  return <td data-testid="person-active">{isActive(person, all, today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonColumns.fill({ order: 12, Component: () => <th>Active</th> }),
  PersonCells.fill({ order: 12, Component: PersonActive }),
];
