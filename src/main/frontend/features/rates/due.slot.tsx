import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople, type Person } from '../people/people';
import { sumOf, useClaims, type Claim } from '../claims/claims';
import { dueOf, rateOn, sumDue, useRates, type Rate } from './rates';

// A person's due-total: their claimed times their grade's rate in force on the sheet's today, divided by
// 100, rounded half up (n/a when no rate is in force); and the sheet's due-total, everyone's added up,
// skipping n/a.
function personDue(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined): string {
  return dueOf(sumOf(claims.filter((c) => c.personId === person.id)), rateOn(rates, person.grade, today));
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
