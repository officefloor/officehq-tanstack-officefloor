import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from './people';
import { useSheetToday } from '../sheet/sheet';
import { isActive, useClaims } from '../claims/claims';

function ActiveHeader() {
  return <th>Active</th>;
}

// Whether the person is active: yes when they have an OPEN or PENDING claim that counts, dated within the 30 days up
// to and including the sheet's today.
function PersonActive({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: claims = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  return <td data-testid="person-active">{isActive(claims, person, today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 5, Component: ActiveHeader }),
  PersonColumns.fill({ order: 5, Component: PersonActive }),
];
