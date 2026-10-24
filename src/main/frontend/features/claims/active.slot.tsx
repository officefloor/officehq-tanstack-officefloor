import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useSheetToday } from '../session/session';
import type { Person } from '../people/people';
import { countsLive, holdsOf, isOpen, useClaims, type Claim } from './claims';

/** How many days back from the sheet's today (today included) a claim keeps its person active. */
const ACTIVE_DAYS = 30;

/** The day a number of days before a date (YYYY-MM-DD). */
const daysBefore = (day: string, n: number) =>
  new Date(Date.parse(`${day}T00:00:00Z`) - n * 86_400_000).toISOString().slice(0, 10);

// Whether a person is active: they have an OPEN or PENDING claim that counts (not deferred, not an unbound one dated
// within a hold of theirs) dated within the 30 days up to and including the sheet's today.
export function isActive(person: Person, claims: Claim[], today: string | undefined): boolean {
  if (today === undefined) return false;
  const since = daysBefore(today, ACTIVE_DAYS - 1);
  const held = holdsOf([person]);
  return claims.some((c) => c.personId === person.id && isOpen(c) && countsLive(c, held, today)
    && since <= c.date && c.date <= today);
}

function ActiveHeader() {
  return <th>Active</th>;
}

function PersonActive({ person }: { person: Person }) {
  const claims = useClaims();
  const today = useSheetToday();
  return <td data-testid="person-active">{isActive(person, claims.data ?? [], today) ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 16.5, Component: ActiveHeader }),
  PersonCells.fill({ order: 16.5, Component: PersonActive }),
];
