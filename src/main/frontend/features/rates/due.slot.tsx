import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople, type Person } from '../people/people';
import { countsLive, holdsOf, isBound, useClaims, type Claim } from '../claims/claims';
import { useSettings, type Settings } from '../roles/roles';
import { chargedOf, formatCents, rateOn, sumDue, useRates, type Rate } from './rates';

// A person's due-total: the sum of their counted claims' dues, each already rounded (a settled claim's
// frozen due, else the figure their last statement bound it to, else its live due, at least the sheet's floor; plus
// an overdue claim's late charge; each claim at the grade they had on its date) — 0.00 with nothing that counts.
// When their grade in force on the sheet's today has no rate in force then it is n/a, unless a counted claim keeps
// a figure — settled, bound, or live at another grade they had on its date, or live
// on or before the sheet's today at a rate that has ended since: then it is the sum of the figures there are. It is at most
// the sheet's cap: above it, it shows the cap and their excess the difference (n/a is not capped). Their unbound OPEN claims dated within a hold of theirs do not count. The sheet's due-total is everyone's added up, skipping n/a.
export function personDue(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): string {
  return capped(personFigures(person, claims, rates, today, settings), settings).due;
}

/** A person's excess: how much the sum of their figures is above the sheet's cap; 0.00 when it is not (or n/a). */
export function personExcess(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): string {
  return capped(personFigures(person, claims, rates, today, settings), settings).excess;
}

/** A sum of figures held to the sheet's cap ("" for none), and how much it was above it; n/a is not capped. */
function capped(sum: string, settings: Settings | undefined): { due: string; excess: string } {
  const cap = settings?.cap ?? '';
  if (sum === 'n/a' || cap === '' || Number(sum) <= Number(cap)) return { due: sum, excess: '0.00' };
  return { due: sumDue([cap]), excess: subtract(sum, cap) };
}

/** The difference of two two-decimal amounts, exactly. */
function subtract(a: string, b: string): string {
  const cents = (s: string) => BigInt(Math.round(Number(s) * 100));
  return formatCents(cents(a) - cents(b));
}

/** The sum of a person's figures, before the cap. */
function personFigures(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined): string {
  const mine = claims.filter((c) => c.personId === person.id && countsLive(c, holdsOf([person]), today));
  const grade = gradeOn(person, today);
  const kept = (c: Claim) => c.due !== null || (isBound(c) && c.statedDue !== null)
    || (!isBound(c) && (gradeOn(person, c.date) !== grade || (today !== undefined && c.date <= today)) && chargedOf(c, rates, gradeOn(person, c.date), settings, today) !== 'n/a');
  if (rateOn(rates, grade, today) === undefined && !mine.some(kept)) {
    return 'n/a';
  }
  return sumDue(mine.map((c) => chargedOf(c, rates, gradeOn(person, c.date), settings, today)));
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

function ExcessHeader() {
  return <th>Excess</th>;
}

function PersonExcess({ person }: { person: Person }) {
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  return <td data-testid="person-excess">{personExcess(person, claims.data ?? [], rates.data, today, settings.data)}</td>;
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
  PeopleColumns.fill({ order: 15.5, Component: ExcessHeader }),
  PersonCells.fill({ order: 15.5, Component: PersonExcess }),
  HomeMain.fill({ order: 31, Component: DueTotal }),
];
