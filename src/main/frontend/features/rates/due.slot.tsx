import { HomeMain } from '../../slots/defs/homeMain';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople, type Person } from '../people/people';
import { countsLive, holdsOf, isBound, useClaims, type Claim } from '../claims/claims';
import { useSettings, type Settings } from '../roles/roles';
import { chargedOf, formatCents, hundredths, rateOn, sharePart, sumDue, trimmedSum, useRates, type Rate } from './rates';
import { useColumnShown } from '../people/columns';

// A person's due-total: the sum of their counted claims' dues, each already rounded (a settled claim's
// frozen due, else the figure their last statement bound it to, else its live due, at least the sheet's floor; plus
// an overdue claim's late charge; each claim at the grade they had on its date) — 0.00 with nothing that counts.
// When their grade in force on the sheet's today has no rate in force then it is n/a, unless a counted claim keeps
// a figure — settled, bound, or live at another grade they had on its date, or live
// on or before the sheet's today at a rate that has ended since: then it is the sum of the figures there are. It is at most
// the sheet's cap: above it, it shows the cap and their excess the difference (n/a is not capped). Their unbound OPEN claims dated within a hold of theirs do not count. The sheet's due-total is everyone's added up, skipping n/a and, with three or more figures, the highest and the lowest.
// A claim shared with other people gives each holder their part of its due (its due times their share divided by
// 100, rounded half to even): it counts in their due-total (theirs is just their parts when they would otherwise
// have none, n/a), and its own person's counts its due less the holders' parts — before the cap.
export function personDue(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined, people: Person[] | undefined): string {
  return capped(withShares(person, claims, rates, today, settings, people), settings).due;
}

/** A person's excess: how much the sum of their figures is above the sheet's cap; 0.00 when it is not (or n/a). */
export function personExcess(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined, people: Person[] | undefined): string {
  return capped(withShares(person, claims, rates, today, settings, people), settings).excess;
}

/** The sum of a person's figures with the shares: their own claims' holders' parts taken off, and their parts of
 *  the claims shared with them added (theirs alone when the sum is n/a). */
function withShares(person: Person, claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined, people: Person[] | undefined): string {
  const sum = personFigures(person, claims, rates, today, settings);
  let given = 0n;
  let held: bigint | null = null;
  for (const c of claims) {
    if ((c.shares ?? []).length === 0) continue;
    const mineToGive = c.personId === person.id;
    const share = (c.shares ?? []).find((s) => s.personId === person.id);
    if (!mineToGive && !share) continue;
    const owner = mineToGive ? person : (people ?? []).find((p) => p.id === c.personId);
    if (!owner || !countsLive(c, holdsOf([owner]), today)) continue;
    const due = chargedOf(c, rates, gradeOn(owner, c.date), settings, today);
    if (due === 'n/a') continue;
    if (mineToGive) {
      for (const s of c.shares) given += sharePart(due, s.share);
    } else if (share) {
      held = (held ?? 0n) + sharePart(due, share.share);
    }
  }
  if (sum === 'n/a') return held === null ? sum : formatCents(held);
  return formatCents(hundredths(sum) - given + (held ?? 0n));
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
  if (!useColumnShown('due-total')) return null;
  return <th>Due</th>;
}

function PersonDue({ person }: { person: Person }) {
  const shown = useColumnShown('due-total');
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  if (!shown) return null;
  return <td data-testid="person-due-total">{personDue(person, claims.data ?? [], rates.data, today, settings.data, people.data)}</td>;
}

function ExcessHeader() {
  if (!useColumnShown('excess')) return null;
  return <th>Excess</th>;
}

function PersonExcess({ person }: { person: Person }) {
  const shown = useColumnShown('excess');
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  if (!shown) return null;
  return <td data-testid="person-excess">{personExcess(person, claims.data ?? [], rates.data, today, settings.data, people.data)}</td>;
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
  const total = trimmedSum((people.data ?? []).map((p) => personDue(p, claims.data ?? [], rates.data, today, settings.data, people.data)));
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
