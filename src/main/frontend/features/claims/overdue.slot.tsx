import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { DEFAULT_TERMS, useSettings } from '../roles/roles';
import { figureOf, sumDue, useRates, type Rate } from '../rates/rates';
import { countsLive, heldIds, isOverdue, useClaims, type Claim } from './claims';

// A person's overdue count and overdue-total: their counted claims that are overdue (OPEN, due-by before the
// sheet's today; a held person's OPEN claims do not count), and those claims' figures added up, skipping n/a.
// The sheet's are everyone's added up.
function overdueOf(person: Person, claims: Claim[], rates: Rate[] | undefined, terms: string, today: string | undefined,
  floor: string | undefined): { count: number; total: string } {
  const held = heldIds([person]);
  const mine = claims.filter((c) => c.personId === person.id && countsLive(c, held) && isOverdue(c, terms, today));
  return { count: mine.length, total: sumDue(mine.map((c) => figureOf(c, rates, person.grade, floor))) };
}

function useOverdue() {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  return (person: Person) => overdueOf(person, claims.data ?? [], rates.data, settings.data?.terms ?? DEFAULT_TERMS,
    today, settings.data?.floor);
}

function OverdueHeader() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  );
}

function PersonOverdue({ person }: { person: Person }) {
  const { count, total } = useOverdue()(person);
  return (
    <>
      <td data-testid="person-overdue-count">{count}</td>
      <td data-testid="person-overdue-total">{total}</td>
    </>
  );
}

function OverdueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const overdue = useOverdue();
  if (!user || !sheet) return null;
  const each = (people.data ?? []).map(overdue);
  return (
    <p>
      Overdue <strong data-testid="rollup-overdue-count">{each.reduce((n, o) => n + o.count, 0)}</strong>{' '}
      totalling <strong data-testid="rollup-overdue-total">{sumDue(each.map((o) => o.total))}</strong>
    </p>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 16, Component: OverdueHeader }),
  PersonCells.fill({ order: 16, Component: PersonOverdue }),
  HomeMain.fill({ order: 32, Component: OverdueTotal }),
];
