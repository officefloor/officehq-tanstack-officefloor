import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { counts, useClaims, type Claim } from '../claims/claims';
import { dueOf, rateOn, sumDue, useRates, type Rate } from './rates';

// A person's due-total: the sum of their counted claims' figures, each already rounded (a settled claim's
// frozen due, else its live due) — 0.00 with nothing that counts. When their grade has no rate in force on
// the sheet's today it is n/a, unless a counted settled claim keeps a figure: then it is the sum of the
// figures there are. The sheet's due-total is everyone's added up, skipping n/a.
function personDue(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined): string {
  const mine = claims.filter((c) => c.personId === person.id && counts(c));
  if (rateOn(rates, person.grade, today) === undefined && !mine.some((c) => c.due !== null)) return 'n/a';
  return sumDue(mine.map((c) => c.due ?? dueOf(c.amount, rateOn(rates, person.grade, c.date))));
}

function DueHeader() {
  return <th>Due</th>;
}

function PersonDue({ person }: { person: Person }) {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  return <td data-testid="person-due-total">{personDue(person, claims.data ?? [], rates.data, today)}</td>;
}

function DueTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  if (!user || !sheet) return null;
  const total = sumDue((people.data ?? []).map((p) => personDue(p, claims.data ?? [], rates.data, today)));
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
