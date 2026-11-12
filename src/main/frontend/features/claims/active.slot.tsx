import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { isActive, live, useClaims } from './claims';

// Whether a person is active: an OPEN or PENDING claim that counts, dated within the last 30 days.
function ActiveHead() {
  return <th>Active</th>;
}

function PersonActive({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const today = useSheetToday(sheet);
  const active = isActive(live((claims.data ?? []).filter((c) => c.personId === person.id), [person]), today);
  return <td data-testid="person-active">{active ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PersonColumnHeads.fill({ order: 12, Component: ActiveHead }),
  PersonColumns.fill({ order: 12, Component: PersonActive }),
];
