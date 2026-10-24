import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { countsLive, heldIds, useClaims, type Claim } from '../claims/claims';
import { useSettings, type Settings } from '../roles/roles';
import { chargedOf, rateOn, sumDue, useRates, type Rate } from './rates';

// A person's due-total: the sum of their counted claims' dues, each already rounded (a settled claim's
// frozen due, else the figure their last statement bound it to, else its live due, at least the sheet's floor; plus
// an overdue claim's late charge) — 0.00 with nothing that counts. When their grade has no rate in force on
// the sheet's today it is n/a, unless a counted settled or bound claim keeps a figure: then it is the sum of the
// figures there are. While they are held their OPEN claims do not count. The sheet's due-total is everyone's added up, skipping n/a.
export function personDue(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): string {
  const mine = claims.filter((c) => c.personId === person.id && countsLive(c, heldIds([person])));
  if (rateOn(rates, person.grade, today) === undefined && !mine.some((c) => c.due !== null || (c.stated && c.statedDue !== null))) {
    return 'n/a';
  }
  return sumDue(mine.map((c) => chargedOf(c, rates, person.grade, settings, today)));
}

function DueHeader() {
  return <th>Due</th>;
}

function PersonDue({ person }: { person: Person }) {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  return <td data-testid="person-due-total">{personDue(person, claims.data ?? [], rates.data, today, settings.data)}</td>;
}

function DueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  if (!user || !sheet) return null;
  const total = sumDue((people.data ?? []).map((p) => personDue(p, claims.data ?? [], rates.data, today, settings.data)));
  return (
    <p>
      Due in total <strong data-testid="rollup-due-total">{total}</strong>
    </p>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 15, Component: DueHeader }),
  PersonCells.fill({ order: 15, Component: PersonDue }),
  HomeMain.fill({ order: 31, Component: DueTotal }),
];
