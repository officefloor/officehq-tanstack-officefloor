import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import { type Person } from '../people/people';
import { useLate } from '../settings/settings';
import { isActive } from './active';

function ActiveHead() {
  return <th>Active</th>;
}

/** Whether the person is active: yes or no (see isActive). */
function PersonActive({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const late = useLate(sheetId);
  return <td data-testid="person-active">{isActive(person, claims.data, late.today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PeopleHead.fill({ order: 27, Component: ActiveHead }),
  PersonRow.fill({ order: 27, Component: PersonActive }),
];
