import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import { isActive } from './active';

function ActiveHead() {
  return <th>Active</th>;
}

// Whether the person has an open claim that counts, dated within the last 30 days.
function PersonActive({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const today = useSheetToday(sheetId);
  return <td data-testid="person-active">{isActive(claims, person.holds, today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonHead.fill({ order: 26, Component: ActiveHead }),
  PersonRow.fill({ order: 26, Component: PersonActive }),
];
