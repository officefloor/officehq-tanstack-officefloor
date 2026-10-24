import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useSheetToday } from '../session/session';
import { gradeOn, type Person } from '../people/people';
import { useSettings, type Settings } from '../roles/roles';
import { chargedOf, sumDue, useRates, type Rate } from '../rates/rates';
import { countsLive, holdsOf, isOpen, useClaims, type Claim } from './claims';

/** A claim's age on the sheet's today: the days from its date (a claim dated after today is 0). */
function ageOf(c: Claim, today: string | undefined): number {
  if (today === undefined) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${c.date}T00:00:00Z`)) / 86_400_000;
  return Math.max(0, Math.round(days));
}

// A person's aged dues: the dues (with their late charge, skipping n/a) of their counted OPEN and PENDING claims,
// split by the claim's age on the sheet's today — 0 to 30 days, 31 to 60, and 61 or more (a claim dated after today is 0 to 30).
export function agedOf(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): { upTo30: string; upTo60: string; over60: string } {
  const held = holdsOf([person]);
  const mine = claims.filter((c) => c.personId === person.id && isOpen(c) && countsLive(c, held, today));
  const sum = (inBucket: (age: number) => boolean) => sumDue(mine.filter((c) => inBucket(ageOf(c, today)))
    .map((c) => chargedOf(c, rates, gradeOn(person, c.date), settings, today)));
  return { upTo30: sum((a) => a <= 30), upTo60: sum((a) => a > 30 && a <= 60), over60: sum((a) => a > 60) };
}

function AgedHeader() {
  return (
    <>
      <th>0–30 days</th>
      <th>31–60 days</th>
      <th>61+ days</th>
    </>
  );
}

function PersonAged({ person }: { person: Person }) {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  const { upTo30, upTo60, over60 } = agedOf(person, claims.data ?? [], rates.data, today, settings.data);
  return (
    <>
      <td data-testid="person-aged-0-30">{upTo30}</td>
      <td data-testid="person-aged-31-60">{upTo60}</td>
      <td data-testid="person-aged-61">{over60}</td>
    </>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 16.2, Component: AgedHeader }),
  PersonCells.fill({ order: 16.2, Component: PersonAged }),
];
