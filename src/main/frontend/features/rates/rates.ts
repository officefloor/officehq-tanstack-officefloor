import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { gradeOn, liveHolds } from '../people/people';
import type { Grade, GradeFrom, Hold, Person } from '../people/people';
import { counts, isBound, isCorrection, isDeferred, isOpen } from '../claims/claims';
import type { Claim, ClaimStatus } from '../claims/claims';

/**
 * A grade's rate: a percent with two decimals, from a date (yyyy-MM-dd; '' when it applies from the
 * beginning of time) through a date (yyyy-MM-dd; '' when it has no end), and its level: 0 when set
 * on the sheet itself, 1 on its parent sheet, and so on up to MAX_LEVEL, then on past them the sheet
 * it was cloned from and its own; and its version: how many
 * times a rate has been set for its grade and from-date (it is the newest of them). A forward-only
 * rate (saved with a from-date on or after the sheet's today) applies only to claims added after it
 * was saved: those whose id is above `addedAfter` ('' when it applies to every claim); an older claim
 * sees the version it `replaced` (null for none) instead.
 */
export type Rate = {
  grade: Grade; pct: string; from: string; to: string; level: number; version: number;
  addedAfter?: string; replaced?: Rate | null;
};

/** How many parent sheets up a rate is looked for. */
export const MAX_LEVEL = 3;

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, by grade then from-date (none first), then its parent sheets', each at its level. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: async () =>
      (await getJson<{ rates: Rate[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`)).rates,
  });
}

/**
 * The version of a rate that applies to a claim (by its id): the rate unless it is forward-only and
 * the claim was on the sheet when it was saved, then the one it replaced, and so on; null when none does.
 */
function forClaim(rate: Rate, claimId: string): Rate | null {
  if (!rate.addedAfter || !/^\d+$/.test(claimId) || BigInt(claimId) > BigInt(rate.addedAfter)) return rate;
  return rate.replaced ? forClaim(rate.replaced, claimId) : null;
}

/**
 * A grade's rate in force on a date (yyyy-MM-dd): the sheet's own, else its parent sheet's, and so
 * on up the levels (past the parents, the sheet it was cloned from's). Null when no level has one in force. For a claim (`claimId`), only the rates
 * that apply to it count (a forward-only rate applies only to claims added after it was saved);
 * without one, every rate in force counts, as the rates panel shows them.
 */
export function rateOn(rates: Rate[], grade: Grade, date: string, claimId?: string): string | null {
  const applying = claimId === undefined ? rates
    : rates.map((r) => forClaim(r, claimId)).filter((r): r is Rate => r !== null);
  const top = applying.reduce((max, r) => Math.max(max, r.level ?? 0), 0);
  for (let level = 0; level <= top; level++) {
    const pct = rateAt(applying, grade, date, level);
    if (pct !== null) return pct;
  }
  return null;
}

/**
 * A grade's rate in force on a date at one level: the one whose from-date is the latest on or
 * before it, unless its to-date is before the date. Null when the date is before every rate the
 * grade has there (or it has none), or after the to-date of the rate that would apply.
 */
function rateAt(rates: Rate[], grade: Grade, date: string, level: number): string | null {
  let best: Rate | null = null;
  for (const r of rates) {
    if ((r.level ?? 0) !== level || r.grade !== grade || r.from > date) continue;
    if (best === null || r.from > best.from) best = r;
  }
  if (best === null || (best.to !== '' && best.to < date)) return null;
  return best.pct;
}

/** The sheet's rate for a grade on a date, or null when none is in force (or the rates are not read yet). */
export function useGradeRate(sheetId: string, grade: Grade, date: string): string | null {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/**
 * A dated setting: its starting value, in force before every recorded value, and the values recorded
 * for it, each from a date (yyyy-MM-dd).
 */
export type Dated = { start: string; values: { from: string; value: string }[] };

/** A dated setting's value in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it, else its start. */
export function valueOn(dated: Dated, date: string): string {
  let best: { from: string; value: string } | null = null;
  for (const v of dated.values) {
    if (v.from > date) continue;
    if (best === null || v.from > best.from) best = v;
  }
  return best === null || best.value === '' ? dated.start : best.value;
}

/** A percent as a rate takes it: 0.00 or more, with up to two decimals. */
export const PCT = /^\d+(\.\d{1,2})?$/;

export const NA = 'n/a';

/** A two-decimal number (negative for a correction) as a whole count of hundredths. */
function hundredths(value: string): bigint {
  const trimmed = value.trim();
  const negative = trimmed.startsWith('-');
  const [whole, frac = ''] = (negative ? trimmed.slice(1) : trimmed).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function showCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${(abs % 100n).toString().padStart(2, '0')}`;
}

/**
 * An amount times a rate, divided by 100, rounded to two decimals (half up when `halfUp`, else half
 * to even), raised to the sheet's `floor` when below it; n/a with no rate (no floor applies to n/a).
 * Worked in whole numbers so no floating-point error creeps in. A correction (a negative amount)
 * takes no floor (`floor` null).
 */
export function due(amount: string, pct: string | null, floor: string | null, halfUp: boolean): string {
  if (pct === null) return NA;
  const rounded = percentOf(amount, pct, 1n, halfUp);
  if (floor === null) return showCents(rounded);
  const least = hundredths(floor);
  return showCents(rounded < least ? least : rounded);
}

/**
 * `times` × a percent of a two-decimal figure, in cents, rounded half up when `halfUp`, else half
 * to even (either symmetric about 0).
 */
function percentOf(figure: string, pct: string, times: bigint, halfUp: boolean): bigint {
  // cents × basis points is the due in millionths of a unit: 10000 of them to the cent.
  const signed = hundredths(figure) * hundredths(pct) * times;
  const x = signed < 0n ? -signed : signed;
  const cents = x / 10000n;
  const rest = x % 10000n;
  const rounded = rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n)) ? cents + 1n : cents;
  return signed < 0n ? -rounded : rounded;
}

/**
 * What the late charge and a claim's rounding need: the sheet's late percent (two decimals, dated: a
 * claim takes the one in force on its date), its
 * today (yyyy-MM-dd; '' until read) and its policy date (yyyy-MM-dd; '' for its
 * today), and each grade's grace days (dated, '' for none: a claim takes those of its grade, the one in
 * force on its date, in force on its date), and its rounding mode (dated, half-up or half-even, '' for
 * none: a claim rounds by the one in force on its date, else by the policy date).
 */
export type Late = { pct: Dated; today: string; policyDate: string; grace: Record<string, Dated>; rounding: Dated };

/**
 * The day a claim's overdue count starts from: its due-by plus the grace days of its `grade` (the one
 * in force on its date) in force on its date, or its due-by when that grade has none. A claim is
 * overdue when this is before the sheet's today, and its late charge's 30-day boundaries are counted
 * from it. Its due-by as shown stays as it is.
 */
export function overdueFrom(claim: Claim, grade: string, late: Late): string {
  const grace = late.grace[grade];
  const days = grace ? valueOn(grace, claim.date) : '';
  return days === '' ? claim.dueBy : addDays(claim.dueBy, Number(days));
}

/**
 * Whether a claim's live figures round half up: by the rounding mode in force on the date it rounds
 * by when there is one; else it rounds by a date (its own, or for a correction
 * the date of the claim it corrects) before the sheet's policy date (its today when blank); on or
 * after it they round half to even.
 */
export function roundsHalfUp(claim: Claim, late: Late): boolean {
  const mode = valueOn(late.rounding, claim.roundDate || claim.date);
  if (mode !== '') return mode === 'half-up';
  const policy = late.policyDate !== '' ? late.policyDate : late.today;
  return (claim.roundDate || claim.date) < policy;
}

/** Whole days from one date (yyyy-MM-dd) to another, worked in UTC so no zone shifts it. */
export function daysBetween(from: string, to: string): number {
  const utc = (date: string) => {
    const [y, m, d] = date.split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  };
  return Math.round((utc(to) - utc(from)) / 86_400_000);
}

/** A date (yyyy-MM-dd) some whole days on, worked in UTC so no zone shifts it. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

/**
 * A claim's late charge on its `figure`: for an OPEN or PENDING claim past its due-by (fixed when it was
 * made) plus its grade's grace ({@link overdueFrom}) on the sheet's today, for every full 30 days it is
 * overdue the late percent in force on that boundary date (that day plus 30, 60, 90 ... days) of the figure plus the
 * charges of the boundaries before it (compounding), each rounded as the claim rounds, added up; else nothing (as for a claim deferred on the sheet's today, or one whose late charge a steward waived, whatever its overdue state). While a statement binds it with a late charge, that charge, whatever the
 * sheet's today; else it follows the sheet's today.
 */
function lateCharge(claim: Claim, figure: string, late: Late, grade: string): bigint {
  if (!isOpen(claim) || isDeferred(claim, late.today) || figure === NA || late.today === '' || isCorrection(claim) || claim.waived) return 0n;
  if (isBound(claim) && claim.statedLate !== '') return hundredths(claim.statedLate);
  const from = overdueFrom(claim, grade, late);
  const overdue = daysBetween(from, late.today);
  const periods = overdue > 0 ? Math.floor(overdue / 30) : 0;
  const halfUp = roundsHalfUp(claim, late);
  let charge = 0n;
  for (let k = 1; k <= periods; k++) {
    const pct = valueOn(late.pct, addDays(from, 30 * k));
    if (hundredths(pct) !== 0n) charge += percentOf(showCents(hundredths(figure) + charge), pct, 1n, halfUp);
  }
  return charge;
}

/** A share (a percent, two decimals) of a two-decimal figure, rounded half to even. */
export function shareOf(figure: string, share: string): string {
  return showCents(percentOf(figure, share, 1n, false));
}

/** Due values added up, skipping n/a. */
export function sumDue(values: string[]): string {
  return showCents(values.filter((v) => v !== NA).reduce((n, v) => n + hundredths(v), 0n));
}

/** One two-decimal figure minus another, two decimals, a minus sign when negative. */
export function diffDue(value: string, previous: string): string {
  return showCents(hundredths(value) - hundredths(previous));
}

/**
 * People's figures added up as a sheet's total, skipping n/a: with three or more figures, the single
 * highest and the single lowest are left out.
 */
export function trimmedSumDue(values: string[]): string {
  const figures = values.filter((v) => v !== NA).map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return showCents(kept.reduce((n, v) => n + v, 0n));
}

/**
 * A claim's due: the one fixed when it was settled (its late charge frozen in), else its live due:
 * its floored figure (the one its person's last statement bound it to (OPEN or PENDING alike), else
 * the greater of the sheet's `floor` in force on its date and its amount at the rate in force on its
 * date) plus the late charge an overdue claim gains on that floored figure (by the grace of its
 * `grade`, the one in force on its date). A correction takes no floor; n/a stays n/a.
 */
export function claimDue(claim: Claim, pct: string | null, floor: Dated, late: Late, grade: string): string {
  if (claim.due !== '') return claim.due;
  const figure = isBound(claim)
    ? claim.statedDue
    : due(claim.amount, pct, isCorrection(claim) ? null : valueOn(floor, claim.date), roundsHalfUp(claim, late));
  if (figure === NA) return figure;
  return showCents(hundredths(figure) + lateCharge(claim, figure, late, grade));
}

/**
 * A person's due-total: the figures of their claims that count (an unbound OPEN one not when dated within one of `holds`: a retired person's, see `liveHolds`) (each at the rate in force on its
 * date for their grade in force on its date), added up, skipping n/a. With no rate for their grade
 * today it is n/a, unless a claim that counts keeps a fixed figure (settled, or bound by a
 * statement), has a figure under another grade in force on its date, or has a figure dated on or
 * before today (under a rate that has since ended). A shared claim counts its due less its holders'
 * parts; the parts `held` of others' claims shared with them count too (and keep it from n/a). Held to
 * the sheet's `cap` ('' for none) less their `cut` total (not below 0.00); n/a is not capped.
 */
export function personDueTotal(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
  cap: string,
  held: HeldPart[] = [],
  cut = '0.00',
): string {
  const total = uncappedDueTotal(rates, grades, today, claims, holds, floor, late, held);
  if (total === NA || cap === '') return total;
  const left = hundredths(cap) - hundredths(cut);
  const room = left > 0n ? left : 0n;
  return hundredths(total) <= room ? total : showCents(room);
}

/**
 * How far a person's due-total, before the sheet's `cap` ('' for none), plus their `cut` total goes above
 * the cap: 0.00 when it does not.
 */
export function personExcess(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
  cap: string,
  held: HeldPart[] = [],
  cut = '0.00',
): string {
  const total = uncappedDueTotal(rates, grades, today, claims, holds, floor, late, held);
  if (total === NA || cap === '') return '0.00';
  const figures = hundredths(total) + hundredths(cut);
  return figures <= hundredths(cap) ? '0.00' : showCents(figures - hundredths(cap));
}

function uncappedDueTotal(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
  held: HeldPart[],
): string {
  const counted = claims.filter((c) => counts(c, holds, today));
  const grade = gradeOn(grades, today);
  const kept = (c: Claim) => c.due !== '' || (isBound(c) && c.statedDue !== NA)
    || (gradeOn(grades, c.date) !== grade && rateOn(rates, gradeOn(grades, c.date), c.date, c.id) !== null)
    || (c.date <= today && rateOn(rates, gradeOn(grades, c.date), c.date, c.id) !== null);
  if (rateOn(rates, grade, today) === null && !counted.some(kept) && held.length === 0) return NA;
  const own = counted.map((c) => ownPart(c, claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date, c.id), floor, late, gradeOn(grades, c.date))));
  return sumDue([...own, ...held.map((h) => h.due)]);
}

/** A holder's part of a claim shared with them: its due times their share / 100, and the claim's status. */
export type HeldPart = { due: string; status: ClaimStatus };

/** A claim's due less its holders' parts (each its due times their share / 100, rounded half to even); n/a stays n/a. */
export function ownPart(claim: Claim, due: string): string {
  if (due === NA || claim.shares.length === 0) return due;
  return showCents(claim.shares.reduce((n, s) => n - hundredths(shareOf(due, s.share)), hundredths(due)));
}

/**
 * The parts a person holds of others' claims shared with them, each claim counting as it does for its
 * own person (at its rate, by its own person's grade and holds), skipping n/a.
 */
export function heldParts(
  personId: string,
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  today: string,
  floor: Dated,
  late: Late,
): HeldPart[] {
  const parts: HeldPart[] = [];
  for (const c of claims) {
    const share = c.shares.find((s) => s.personId === personId);
    const owner = share && people.find((p) => p.id === c.personId);
    if (!share || !owner || !counts(c, liveHolds(owner, today), today)) continue;
    const grade = gradeOn(owner.grades, c.date);
    const due = claimDue(c, rateOn(rates, grade, c.date, c.id), floor, late, grade);
    if (due !== NA) parts.push({ due: shareOf(due, share.share), status: c.status });
  }
  return parts;
}


/**
 * A person's due-total split by status, before the sheet's cap: their settled-total (the dues of
 * their SETTLED claims that count, at their frozen figures, corrections included) and their
 * open-total (the dues of their OPEN and PENDING claims that count), each added up, skipping n/a. A
 * shared claim counts its due less its holders' parts; the parts `held` of others' claims count too.
 */
export function personSplitTotals(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
  held: HeldPart[] = [],
): { settled: string; open: string } {
  const counted = claims.filter((c) => counts(c, holds, today));
  const dueOf = (c: Claim) => ownPart(c, claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date, c.id), floor, late, gradeOn(grades, c.date)));
  const heldIn = (statuses: ClaimStatus[]) => held.filter((h) => statuses.includes(h.status)).map((h) => h.due);
  return {
    settled: sumDue([...counted.filter((c) => c.status === 'SETTLED').map(dueOf), ...heldIn(['SETTLED'])]),
    open: sumDue([...counted.filter(isOpen).map(dueOf), ...heldIn(['OPEN', 'PENDING'])]),
  };
}
