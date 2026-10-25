import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople, type Person } from '../people/people';
import { countsLive, holdsOf, isOpen, useClaims, type Claim } from '../claims/claims';
import { useSettings, type Settings } from '../roles/roles';
import { chargedOf, sumDue, useRates, type Rate } from './rates';

// A person's settled-total and open-total: the dues of their counted SETTLED claims (frozen figures, corrections
// included), and of their counted OPEN and PENDING claims (a deferred or retired-and-excluded one left out, as
// from the due-total), each added up skipping n/a. Together they make their due-total before the cap. The sheet's are
// everyone's added up (not trimmed).
export function splitOf(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): { settled: string; open: string } {
  const mine = claims.filter((c) => c.personId === person.id && countsLive(c, holdsOf([person]), today));
  const total = (of: Claim[]) => sumDue(of.map((c) => chargedOf(c, rates, gradeOn(person, c.date), settings, today)));
  return { settled: total(mine.filter((c) => c.status === 'SETTLED')), open: total(mine.filter(isOpen)) };
}

function useSplit() {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  return (person: Person) => splitOf(person, claims.data ?? [], rates.data, today, settings.data);
}

function SplitHeader() {
  return (
    <>
      <th>Settled total</th>
      <th>Open total</th>
    </>
  );
}

function PersonSplit({ person }: { person: Person }) {
  const { settled, open } = useSplit()(person);
  return (
    <>
      <td data-testid="person-settled-total">{settled}</td>
      <td data-testid="person-open-total">{open}</td>
    </>
  );
}

function SplitTotal() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const split = useSplit();
  if (!user || !sheet) return null;
  const each = (people.data ?? []).map(split);
  return (
    <p>
      Settled in total <strong data-testid="rollup-settled-total">{sumDue(each.map((s) => s.settled))}</strong>{' '}
      open in total <strong data-testid="rollup-open-total">{sumDue(each.map((s) => s.open))}</strong>
    </p>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 15.7, Component: SplitHeader }),
  PersonCells.fill({ order: 15.7, Component: PersonSplit }),
  HomeMain.fill({ order: 31.5, Component: SplitTotal }),
];
