import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';
import { counts, isCorrection, isDeferred, isOpen } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { gradeOn } from '../people/peopleQuery';
import type { Grade, Person } from '../people/peopleQuery';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, valueOn } from '../settings/dated';
import type { DatedValue } from '../settings/dated';

/**
 * `rate` is a percent with two decimals ("12.50"), in force from `from` (yyyy-mm-dd; null: from the
 * beginning of time) through `to` (yyyy-mm-dd; null: with no end). A grade may have several, one per
 * from-date; a grade with no rate has no entry.
 */
export type Rate = { grade: Grade; rate: string; from: string | null; to: string | null };

/** Under the sheet's key, so re-loading the sheet re-reads its rates. */
export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

export const ratesQuery = (sheetId: string) => ({
  queryKey: ratesKey(sheetId),
  queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
});

/**
 * The grade's rate on the date (yyyy-mm-dd): the one with the latest from-date on or before it, or undefined when none
 * applies or that one's to-date is before the date (an earlier rate does not take over again).
 */
export function rateFor(rates: Rate[], grade: Grade | undefined, date: string): string | undefined {
  // No from-date sorts as '', before every date.
  const applying = rates.filter((r) => r.grade === grade && (r.from ?? '') <= date);
  const latest = applying.reduce<Rate | undefined>((best, r) => (!best || (r.from ?? '') > (best.from ?? '') ? r : best), undefined);
  return latest && (latest.to === null || date <= latest.to) ? latest.rate : undefined;
}

/** A two-decimal string ("12.50", "-0.30") in whole hundredths, exactly. */
function hundredths(value: string): bigint {
  const v = value.trim();
  const negative = v.startsWith('-');
  const [whole, frac = ''] = (negative ? v.slice(1) : v).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function show(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/** Cents of `amount` times `rate` percent, rounded half to even, or half up (away from zero) when `halfUp` (the rate is 0 or more; a correction's amount is negative). */
export function dueCents(amount: string, rate: string, halfUp = false): bigint {
  const signed = hundredths(amount) * hundredths(rate);
  const product = signed < 0n ? -signed : signed;
  const cents = product / 10000n;
  const rest = product % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n)) ? cents + 1n : cents;
  return signed < 0n ? -rounded : rounded;
}

/** `amount` times `rate` percent with two decimals, or n/a when there is no rate. */
export function due(amount: string, rate: string | undefined, halfUp = false): string {
  return rate === undefined ? 'n/a' : show(dueCents(amount, rate, halfUp));
}

/** Due figures added up, skipping n/a. */
export function sumDue(dues: string[]): string {
  return show(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + hundredths(d), 0n));
}

/** A sheet total of per-person figures: with three or more figures (n/a is none), the single highest and lowest are left out. */
export function trimmedSum(dues: string[]): string {
  const figures = dues.filter((d) => d !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return show(kept.reduce((total, d) => total + d, 0n));
}

/**
 * What a claim's late charge and rounding depend on: the sheet's today (yyyy-mm-dd), its late percents
 * by from-date (two decimals; each 30-day boundary of an overdue claim takes the one in force on that boundary date), its policy date (yyyy-mm-dd; the sheet's today when none is set) and its claims (to find what a correction corrects).
 */
export type Late = { today: string; pct: DatedValue[]; policy: string; claims: Claim[] };

/**
 * Whether the claim's live figures round half up: it is dated before the policy date (on or after it, they round half
 * to even). A correction rounds by the date of the claim it corrects.
 */
export function roundsHalfUp(claim: Claim, { policy, claims }: Late): boolean {
  const corrected = isCorrection(claim) && claim.corrects !== null
    ? claims.find((c) => c.personId === claim.personId && c.number === claim.corrects)
    : undefined;
  return (corrected ?? claim).date < policy;
}

/** An open (OPEN or PENDING) claim, not deferred, whose due-by (fixed when it was made) is before the sheet's today. */
export const isOverdue = (claim: Claim, today: string) =>
  isOpen(claim) && !isDeferred(claim, today) && today !== '' && claim.dueBy < today;

/** The full 30 days the claim has been overdue on the sheet's today (0 when it is not overdue). */
function lateMonths(claim: Claim, { today }: Late): bigint {
  if (!isOverdue(claim, today)) return 0n;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${claim.dueBy}T00:00:00Z`)) / 86_400_000;
  return BigInt(Math.floor(days / 30));
}

/** The date (yyyy-mm-dd) `days` after `date`. */
function addDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/**
 * A claim's figure: a settled claim's frozen due (its late charge frozen in), otherwise its rate figure — the one its
 * person's last statement bound it to (a PENDING claim is not bound: it shows its live figure), otherwise its amount times the rate, on the claim's date, of
 * the grade its person had on that date (rounded by the policy date, see `roundsHalfUp`). An overdue claim's late charge — for every full 30 days overdue, the late
 * percent in force on that boundary date (the due-by plus 30, 60, 90 ... days) of that rate figure, each rounded like the claim, added up — is added on top: a statement
 * never binds it, so it follows the sheet's today. Last, the sum is raised to the sheet's `floor` in force on the claim's date (two decimals).
 * A correction has neither floor nor late charge.
 */
export function figure(claim: Claim, rates: Rate[], person: Person | undefined, floor: DatedValue[], late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  const halfUp = roundsHalfUp(claim, late);
  const bound = claim.stated && claim.status !== 'PENDING'
    ? (claim.statedDue ?? 'n/a')
    : due(claim.amount, rateFor(rates, gradeOn(person, claim.date), claim.date), halfUp);
  if (isCorrection(claim) || bound === 'n/a') return bound;
  const months = lateMonths(claim, late);
  let total = hundredths(bound);
  for (let n = 1n; n <= months; n++) {
    total += dueCents(bound, valueOn(late.pct, addDays(claim.dueBy, Number(n) * 30)) ?? DEFAULT_LATE_PCT, halfUp);
  }
  return floored(show(total), valueOn(floor, claim.date) ?? DEFAULT_FLOOR);
}

function floored(live: string, floor: string): string {
  return live === 'n/a' || hundredths(live) >= hundredths(floor) ? live : show(hundredths(floor));
}

/**
 * A person's figures added up, before the cap: the sum of their counting claims' rounded figures, as each claim shows it. With no rate today and no figure it is n/a; otherwise it is the sum of the figures there are.
 */
export function personSum(person: Person | undefined, people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue[], late: Late): string {
  if (!person) return 'n/a';
  const rate = rateFor(rates, gradeOn(person, today), today);
  const figures = claims
    .filter((c) => c.personId === person.id && counts(c, people, today))
    .map((c) => figure(c, rates, person, floor, late))
    .filter((d) => d !== 'n/a');
  return rate === undefined && figures.length === 0 ? 'n/a' : sumDue(figures);
}

/** A sum of figures held to the sheet's `cap` (two decimals; "" for none); n/a is not capped. */
export function capped(sum: string, cap: string): string {
  return sum === 'n/a' || cap === '' || hundredths(sum) <= hundredths(cap) ? sum : show(hundredths(cap));
}

/** What a sum of figures is above the sheet's `cap`: 0.00 when it is not (or there is no cap, or it is n/a). */
export function excess(sum: string, cap: string): string {
  return sum === 'n/a' ? '0.00' : show(hundredths(sum) - hundredths(capped(sum, cap)));
}

/** A person's due-total: their figures added up (see `personSum`), held to the sheet's `cap` (two decimals; "" for none). */
export function personDue(person: Person | undefined, people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue[], late: Late, cap: string): string {
  return capped(personSum(person, people, claims, rates, today, floor, late), cap);
}

/**
 * A person's recent settled sum: the frozen figures of their SETTLED claims dated within the 90 days up to and including
 * the sheet's today, added up (a claim settled while no rate applied has no figure and is skipped); 0.00 with none.
 */
export function recentSettled(personId: number, claims: Claim[], today: string): string {
  if (today === '') return '0.00';
  const from = addDays(today, -89);
  return sumDue(claims
    .filter((c) => c.personId === personId && c.status === 'SETTLED' && c.settledDue !== null && from <= c.date && c.date <= today)
    .map((c) => c.settledDue as string));
}
