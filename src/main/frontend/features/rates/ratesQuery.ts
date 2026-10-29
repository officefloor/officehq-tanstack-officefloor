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
 * from-date; a grade with no rate has no entry. `level` is the sheet it is the rate of: 0 the sheet's own,
 * 1 its parent's, 2 its parent's parent, up to 3; for a cloned sheet the sheets of its source's lineage follow (the
 * source, its parents and its own source in turn). `version` is how many versions the grade and from-date's rate has
 * had: setting it again keeps the replaced value as an older version, and the one here is always the newest.
 * `claimsAfter` is set on a forward-only rate (saved with a from-date on or after the sheet's today): it applies only to
 * claims with a higher id, those added after it was saved; null (or absent) applies to every claim.
 */
export type Rate = { grade: Grade; rate: string; from: string | null; to: string | null; level: number; version?: number; claimsAfter?: number | null };

/** Under the sheet's key, so re-loading the sheet re-reads its rates. */
export const ratesKey = (sheetId: string) => [...sheetKey(sheetId), 'rates'] as const;

export const ratesQuery = (sheetId: string) => ({
  queryKey: ratesKey(sheetId),
  queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
});

/**
 * The grade's rate on the date (yyyy-mm-dd): the one with the latest from-date on or before it, or undefined when none
 * applies or that one's to-date is before the date (an earlier rate does not take over again). When the sheet's own
 * rates give none, its parent's are used, then its parent's parent's, and so on, then
 * those of the sheet it was cloned from (by `level`). Given a claim's id, the
 * rate is the claim's: forward-only rates saved after the claim was added are passed over; without one it is the rate
 * in force, regardless.
 */
export function rateFor(rates: Rate[], grade: Grade | undefined, date: string, claimId?: number): string | undefined {
  const applying = claimId === undefined ? rates : rates.filter((r) => r.claimsAfter == null || claimId > r.claimsAfter);
  const levels = [...new Set(applying.map((r) => r.level ?? 0))].sort((a, b) => a - b);
  for (const level of levels) {
    const rate = rateOn(applying.filter((r) => (r.level ?? 0) === level), grade, date);
    if (rate !== undefined) return rate;
  }
  return undefined;
}

/** The grade's rate on the date among rates of one sheet (see `rateFor`). */
function rateOn(rates: Rate[], grade: Grade | undefined, date: string): string | undefined {
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

/** Each figure times its percent added up exactly, then rounded half to even to two decimals (n/a figures skipped). */
export function sumOfShares(parts: [figure: string, pct: string][]): string {
  const product = parts.filter(([f]) => f !== 'n/a').reduce((total, [f, pct]) => total + hundredths(f) * hundredths(pct), 0n);
  const abs = product < 0n ? -product : product;
  const cents = abs / 10000n;
  const rest = abs % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && cents % 2n === 1n) ? cents + 1n : cents;
  return show(product < 0n ? -rounded : rounded);
}

/** Due figures added up, skipping n/a. */
export function sumDue(dues: string[]): string {
  return show(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + hundredths(d), 0n));
}

/** One sum less another (two decimals, a minus sign when lower). */
export function difference(sum: string, previous: string): string {
  return show(hundredths(sum) - hundredths(previous));
}

/** A sheet total of per-person figures: with three or more figures (n/a is none), the single highest and lowest are left out. */
export function trimmedSum(dues: string[]): string {
  const figures = dues.filter((d) => d !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return show(kept.reduce((total, d) => total + d, 0n));
}

/**
 * What a claim's late charge and rounding depend on: the sheet's today (yyyy-mm-dd), its late percents
 * by from-date (two decimals; each 30-day boundary of an overdue claim takes the one in force on that boundary date), its policy date (yyyy-mm-dd; the sheet's today when none is set), its rounding modes by from-date (half-up or half-even; blank is none) and its claims (to find what a correction corrects),
 * each grade's grace days by from-date (by grade; a blank value is none) and its people (to find the grade a claim's person had on its date).
 */
export type Late = { today: string; pct: DatedValue[]; policy: string; rounding: DatedValue[]; claims: Claim[]; grace: Partial<Record<Grade, DatedValue[]>>; people: Person[] };

/**
 * Whether the claim's live figures round half up: the rounding mode in force on its date is half-up, or, with none in
 * force, it is dated before the policy date (on or after it, they round half to even). A correction rounds by the date of the claim it corrects.
 */
export function roundsHalfUp(claim: Claim, { policy, claims, rounding }: Late): boolean {
  const corrected = isCorrection(claim) && claim.corrects !== null
    ? claims.find((c) => c.personId === claim.personId && c.number === claim.corrects)
    : undefined;
  const date = (corrected ?? claim).date;
  const mode = valueOn(rounding, date) ?? '';
  return mode !== '' ? mode === 'half-up' : date < policy;
}

/** The date (yyyy-mm-dd) the claim's overdue count runs from: its due-by plus the grace days, in force on its date, of the grade its person had on its date (none: its due-by). */
export function overdueFrom(claim: Claim, late: Late): string {
  const person = late.people.find((p) => p.id === claim.personId);
  const grade = gradeOn(person, claim.date);
  const grace = grade ? valueOn(late.grace[grade], claim.date) ?? '' : '';
  return grace === '' ? claim.dueBy : addDays(claim.dueBy, Number(grace));
}

/** An open (OPEN or PENDING) claim, not deferred, whose due-by (fixed when it was made) plus its grade's grace is before the sheet's today. */
export const isOverdue = (claim: Claim, late: Late) =>
  isOpen(claim) && !isDeferred(claim, late.today) && late.today !== '' && overdueFrom(claim, late) < late.today;

/** The full 30 days the claim has been overdue on the sheet's today, counted from its due-by plus the grace (0 when it is not overdue). */
function lateMonths(claim: Claim, late: Late): bigint {
  if (!isOverdue(claim, late)) return 0n;
  const days = (Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${overdueFrom(claim, late)}T00:00:00Z`)) / 86_400_000;
  return BigInt(Math.floor(days / 30));
}

/** The date (yyyy-mm-dd) `days` after `date`. */
function addDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/**
 * A claim's figure: a settled claim's frozen due (its late charge frozen in), otherwise the due its person's last
 * statement bound it to, as shown then (its late charge and floor included, so it no longer follows the sheet's today),
 * otherwise its amount times the rate, on the claim's date, of
 * the grade its person had on that date (rounded by the policy date, see `roundsHalfUp`), raised to the sheet's `floor` in force on the claim's date (two decimals).
 * An overdue claim's late charge — for every full 30 days overdue, the late percent in force on that boundary date (the due-by plus the grace plus 30, 60, 90 ... days)
 * of that floored figure plus the charges of the boundaries before it (compounding), each rounded like the claim, added up — is then added on top, following the sheet's today.
 * A correction has neither floor nor late charge.
 */
export function figure(claim: Claim, rates: Rate[], person: Person | undefined, floor: DatedValue[], late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  if (claim.stated) return claim.statedDue ?? 'n/a';
  const halfUp = roundsHalfUp(claim, late);
  const bound = due(claim.amount, rateFor(rates, gradeOn(person, claim.date), claim.date, claim.id), halfUp);
  if (isCorrection(claim) || bound === 'n/a') return bound;
  const months = lateMonths(claim, late);
  const from = overdueFrom(claim, late);
  let total = hundredths(floored(bound, valueOn(floor, claim.date) ?? DEFAULT_FLOOR));
  for (let n = 1n; n <= months; n++) {
    total += dueCents(show(total), valueOn(late.pct, addDays(from, Number(n) * 30)) ?? DEFAULT_LATE_PCT, halfUp);
  }
  return show(total);
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

/**
 * A person's OPEN and PENDING claims that count, their figures added up by the claim's age on the sheet's today (the days
 * from its date): 0 to 30 (a claim dated after today included), 31 to 60, and 61 or more. Not capped; n/a figures are skipped.
 */
export function agedDue(person: Person | undefined, people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue[], late: Late): [string, string, string] {
  const buckets: string[][] = [[], [], []];
  if (person) {
    for (const c of claims) {
      if (c.personId !== person.id || !isOpen(c) || !counts(c, people, today)) continue;
      const days = today === '' ? 0 : (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${c.date}T00:00:00Z`)) / 86_400_000;
      buckets[days <= 30 ? 0 : days <= 60 ? 1 : 2].push(figure(c, rates, person, floor, late));
    }
  }
  return [sumDue(buckets[0]), sumDue(buckets[1]), sumDue(buckets[2])];
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

/**
 * A person's figures that count (see `personSum`), split by status: their SETTLED claims' frozen figures, and their
 * OPEN and PENDING claims' figures, each added up (n/a skipped; not capped). The two add up to `personSum`.
 */
export function settledAndOpen(person: Person | undefined, people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue[], late: Late): [string, string] {
  const settled: string[] = [];
  const open: string[] = [];
  if (person) {
    for (const c of claims) {
      if (c.personId !== person.id || !counts(c, people, today)) continue;
      (c.status === 'SETTLED' ? settled : open).push(figure(c, rates, person, floor, late));
    }
  }
  return [sumDue(settled), sumDue(open)];
}

/**
 * A person's score: their settled-total (two decimals) divided by 100, minus 5 for each of their overdue claims that
 * count, rounded half to even to a whole number.
 */
export function score(settled: string, overdueCount: number): string {
  const n = hundredths(settled) - 50000n * BigInt(overdueCount);
  const negative = n < 0n;
  const abs = negative ? -n : n;
  const whole = abs / 10000n;
  const rest = abs % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && whole % 2n === 1n) ? whole + 1n : whole;
  return (negative && rounded !== 0n ? -rounded : rounded).toString();
}

/**
 * What setting the grade's rate to `pct` on every date would change: of the OPEN and PENDING claims of people on the
 * grade (the grade they had on the claim's date), how many would show a different figure, and the sum of the new
 * figures minus the current ones (two decimals; n/a counts as 0.00). A claim a statement bound keeps its figure.
 */
export function ratePreview(grade: Grade, pct: string, people: Person[], claims: Claim[], rates: Rate[], floor: DatedValue[], late: Late): { count: number; diff: string } {
  const candidate: Rate[] = [...rates.filter((r) => r.grade !== grade || r.level !== 0), { grade, rate: pct, from: null, to: null, level: 0 }];
  const cents = (d: string) => (d === 'n/a' ? 0n : hundredths(d));
  let count = 0;
  let diff = 0n;
  for (const c of claims) {
    if (!isOpen(c)) continue;
    const person = people.find((p) => p.id === c.personId);
    if (!person || gradeOn(person, c.date) !== grade) continue;
    const now = figure(c, rates, person, floor, late);
    const then = figure(c, candidate, person, floor, late);
    if (now === then) continue;
    count++;
    diff += cents(then) - cents(now);
  }
  return { count, diff: show(diff) };
}
