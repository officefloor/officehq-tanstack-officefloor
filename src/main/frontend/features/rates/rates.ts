import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, lateFrom, live, overdue, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import type { DatedValue } from '../settings/settings';

/** A grade's rate on the sheet: a percent with two decimals, in force from its from-date (yyyy-mm-dd, or
 *  null: from the beginning of time) through its to-date (null: with no end). A grade may have several, one
 *  per from-date; none when it has no rate. `level` is where it comes from: 0 the sheet's own, 1 its parent
 *  sheet's, 2 the parent's parent's, 3 the one above that; after them, on a cloned sheet, the sheet it was cloned from,
 *  its parents, its own source and so on. `version` is how many have been set for the grade from
 *  that from-date (this the newest, the one in force). A forward-only rate (saved with a from-date on or after the
 *  sheet's today) carries `after`, the highest claim id when it was saved: it reaches only claims added after it (a
 *  higher id); null or absent: every claim. `older` are the grade's older versions from that from-date, newest first:
 *  a claim the rate does not reach uses the newest of them that does. */
export type Rate = { grade: string; rate: string; from: string | null; to: string | null; level?: number; version?: number;
  after?: number | null; older?: OlderRate[] };

/** An older version of a rate (see Rate). */
export type OlderRate = { rate: string; to: string | null; after?: number | null };

/** Whether a rate (or an older version of one) reaches the claim (see Rate); every one does without a claim. */
function reaches(r: { after?: number | null }, claimId: number | undefined): boolean {
  return claimId === undefined || r.after == null || claimId > r.after;
}

/** Of one sheet's rates, the ones in force for the claim: each grade and from-date's newest version that reaches it
 *  (see Rate); all of them as they are without a claim. */
function reaching(rates: Rate[], claimId: number | undefined): Rate[] {
  if (claimId === undefined) return rates;
  return rates.flatMap((r) => {
    if (reaches(r, claimId)) return [r];
    const older = (r.older ?? []).find((o) => reaches(o, claimId));
    return older ? [{ ...r, rate: older.rate, to: older.to, after: older.after }] : [];
  });
}

/** Of the rates, the sheet's own (not its parents'). */
export function ownRates(rates: Rate[]): Rate[] {
  return rates.filter((r) => (r.level ?? 0) === 0);
}

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, every grade's every from-date, then its parents' (up to three levels up), then, on a clone,
 *  those of the sheet it was cloned from and its parents and source. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** A grade's rate in force on a date (yyyy-mm-dd), for the claim with the id when one is given (of the rates that
 *  reach it, see Rate): the one whose from-date is the latest on or before it, a rate with no from-date applying from
 *  the beginning of time; undefined when none applies, or that one's to-date is before the date. When the sheet's own
 *  has none, the nearest level's (parent, or a clone's source) that has one. */
export function rateOn(rates: Rate[], grade: string, date: string, claimId?: number): string | undefined {
  const levels = [...new Set(rates.map((r) => r.level ?? 0))].sort((a, b) => a - b);
  for (const level of levels) {
    const rate = levelRateOn(reaching(rates.filter((r) => (r.level ?? 0) === level), claimId), grade, date);
    if (rate !== undefined) return rate;
  }
  return undefined;
}

/** Of one sheet's rates, the grade's in force on the date (see rateOn). */
function levelRateOn(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  if (found?.to != null && found.to < date) return undefined;
  return found?.rate;
}

/** A grade's rate in force on a date, or undefined when none applies (or the rates are still loading). */
export function useRateFor(sheetId: string, grade: string, date: string): string | undefined {
  return rateOn(useRates(sheetId).data ?? [], grade, date);
}

/** A two-decimal amount (may be negative) as whole cents, exactly. */
export function toCents(amount: string): bigint {
  const trimmed = amount.trim();
  const negative = trimmed.startsWith('-');
  const [whole, frac = ''] = (negative ? trimmed.slice(1) : trimmed).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

function fromCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** One due figure minus another, two decimals, a minus sign when negative. */
export function diffDue(a: string, b: string): string {
  return fromCents(toCents(a) - toCents(b));
}

/** Due on an amount: amount times rate, divided by 100, rounded to two decimals half to even, or half up (away
 *  from zero) when `halfUp` (exact, in integers); 'n/a' when there is no rate. */
export function dueOn(amount: string, rate: string | undefined, halfUp = false): string {
  if (rate === undefined) return 'n/a';
  const scaled = toCents(amount) * toCents(rate); // in 1/10000 of a cent
  const sign = scaled < 0n ? -1n : 1n;
  const abs = scaled * sign;
  let cents = abs / 10000n;
  const rest = abs % 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return fromCents(cents * sign);
}

/** Two-decimal amounts added up exactly, skipping 'n/a'. */
export function sumDue(dues: string[]): string {
  return fromCents(dues.filter((d) => d !== 'n/a').reduce((total, d) => total + toCents(d), 0n));
}

/** A person's due-total: the figures of their claims that count (each as shown on its row) added up;
 *  'n/a' when their grade today has no rate today (rateToday undefined), unless a claim that counts has a
 *  figure (settled, stated, or live at the grade in force on its date), then the figures there are added up. */
export function personDue(claims: Claim[], rateFor: (claim: Claim) => string | undefined,
    rateToday: string | undefined, floor: DatedValue, late: Late): string {
  const counted = claims.filter(counts);
  const dues = counted.map((c) => claimDue(c, rateFor(c), floor, late));
  if (rateToday === undefined && dues.every((d) => d === 'n/a')) return 'n/a';
  return sumDue(dues);
}

/** A person's figures added up, before the cap: their live claims among these (see live) through personDue, at
 *  the grade in force on each claim's date and on the sheet's today. */
export function personDueSum(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late): string {
  return personDue(live(claims.filter((c) => c.personId === person.id), [person], today),
    (c) => rateOn(rates, gradeOn(person, c.date), c.date, c.id), rateOn(rates, gradeOn(person, today), today), floor, late);
}

/** A person's dues of one kind: the due (as shown on its row; a SETTLED one's frozen) of each of their claims that
 *  count in the live totals (see live) and whose status is one of `statuses`, added up, skipping n/a. Their
 *  settled-total (SETTLED) and open-total (OPEN, PENDING) add up to their figures before the cap (see personDueSum). */
export function personStatusSum(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, statuses: Claim['status'][]): string {
  return sumDue(live(claims.filter((c) => c.personId === person.id), [person], today)
    .filter((c) => statuses.includes(c.status))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date, c.id), floor, late)));
}

/** The sheet's due-total for a tag: the due (as shown on its row) of each claim that counts in the live totals
 *  carrying the tag, or whose person carries it (once either way) (see live), every person's added up, skipping n/a; not capped or trimmed. */
export function sheetTagSum(people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, tag: string): string {
  return sumDue(people.flatMap((p) => live(claims.filter((c) => c.personId === p.id && (c.tags.includes(tag) || p.tags.includes(tag))), [p], today)
    .map((c) => claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date, c.id), floor, late))));
}

/** The sheet's due-total for a calendar month (yyyy-mm): the due (as shown on its row) of each claim dated in it that
 *  counts in the live totals (see live), every person's added up, skipping n/a; not capped or trimmed. */
export function sheetMonthSum(people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, month: string): string {
  return sumDue(people.flatMap((p) => live(claims.filter((c) => c.personId === p.id && c.date.slice(0, 7) === month), [p], today)
    .map((c) => claimDue(c, rateOn(rates, gradeOn(p, c.date), c.date, c.id), floor, late))));
}

export const SETTLED_STATUSES: Claim['status'][] = ['SETTLED'];
export const OPEN_STATUSES: Claim['status'][] = ['OPEN', 'PENDING'];

/** The sheet's dues of one kind: every person's (see personStatusSum) added up, not trimmed. */
export function sheetStatusSum(people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, statuses: Claim['status'][]): string {
  return sumDue(people.map((p) => personStatusSum(p, claims, rates, today, floor, late, statuses)));
}

/** A person's due-total as shown on their row: their figures added up (see personDueSum), at most the sheet's
 *  cap ('' when none). */
export function personDueTotal(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, cap: string): string {
  return capped(personDueSum(person, claims, rates, today, floor, late), cap);
}

/** These people's settled figures (each SETTLED claim's due as shown on its row: the one frozen when it was settled)
 *  over their SETTLED claims that count, dated within the 90 days up to and including the sheet's today, added up,
 *  skipping n/a; 0.00 with none (or while today is loading). */
export function recentSettledSum(people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late): string {
  if (today === '') return '0.00';
  const since = new Date(Date.parse(`${today}T00:00:00Z`) - 89 * DAY_MS).toISOString().slice(0, 10);
  const byId = new Map(people.map((p) => [p.id, p]));
  return sumDue(claims
    .filter((c) => byId.has(c.personId) && c.status === 'SETTLED' && counts(c) && c.date >= since && c.date <= today)
    .map((c) => claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId), c.date), c.date, c.id), floor, late)));
}

/** 100.00 split equally among n patrons in the order named: each share 100.00 / n rounded to two decimals half to
 *  even, the first-named taking the remainder (100.00 minus the shares added up). */
export function patronShares(n: number): string[] {
  if (n <= 0) return [];
  const count = BigInt(n);
  let share = 10000n / count;
  const rest = 10000n % count;
  if (rest * 2n > count || (rest * 2n === count && share % 2n === 1n)) share += 1n;
  return Array.from({ length: n }, (_, i) => fromCents(i === 0 ? 10000n - share * (count - 1n) : share));
}

/** Figures each weighted by a share: the sum of figure times share divided by 100, rounded half to even (exact, in
 *  integers); n/a figures are skipped. */
export function sharedSum(parts: { figure: string; share: string }[]): string {
  const scaled = parts.filter((p) => p.figure !== 'n/a')
    .reduce((total, p) => total + toCents(p.figure) * toCents(p.share), 0n); // in 1/10000 of a cent
  const sign = scaled < 0n ? -1n : 1n;
  const abs = scaled * sign;
  let cents = abs / 10000n;
  const rest = abs % 10000n;
  if (rest > 5000n || (rest === 5000n && cents % 2n === 1n)) cents += 1n;
  return fromCents(cents * sign);
}

/** A sum of figures at most the cap ('' when none); 'n/a' is not capped. */
export function capped(sum: string, cap: string): string {
  return sum === 'n/a' || cap === '' || toCents(sum) <= toCents(cap) ? sum : fromCents(toCents(cap));
}

/** How far a sum of figures is above the cap ('' when none); 0.00 when it is not above it, or n/a. */
export function excess(sum: string, cap: string): string {
  return sum === 'n/a' || cap === '' || toCents(sum) <= toCents(cap) ? '0.00' : fromCents(toCents(sum) - toCents(cap));
}

/** Per-person figures added up, skipping n/a, leaving out the single highest and the single lowest figure when
 *  there are three or more. */
export function trimmedSum(figures: string[]): string {
  const sorted = figures.filter((f) => f !== 'n/a').map(toCents).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = sorted.length >= 3 ? sorted.slice(1, -1) : sorted;
  return fromCents(kept.reduce((total, c) => total + c, 0n));
}

/** The sheet's due-total: every person's due-total added up, skipping n/a, trimmed (see trimmedSum). */
export function sheetDueTotal(people: Person[], claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, cap: string): string {
  return trimmedSum(people.map((p) => personDueTotal(p, claims, rates, today, floor, late, cap)));
}

/** The sheet's overdue-total: every person's overdue-total (see overdueDues) added up, trimmed (see trimmedSum). */
export function sheetOverdueTotal(people: Person[], claims: Claim[], rates: Rate[], late: Late,
    floor: DatedValue): string {
  return trimmedSum(people.map((p) => sumDue(overdueDues(claims.filter((c) => c.personId === p.id), [p], rates,
      late, floor))));
}

/** The overdue claims that count among these, with the due of each (as shown on its row). */
export function overdueDues(claims: Claim[], people: Person[], rates: Rate[], late: Late, floor: DatedValue): string[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return live(claims, people, late.today)
    .filter((c) => overdue(c, late.today, late.grace(c)))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId), c.date), c.date, c.id), floor, late));
}

/** What the late charge follows: the sheet's late percent by date (two decimals; a claim gains the one in force
 *  on each 30-day boundary past its due-by plus its grace) and its today; and each claim's grace in days (the grace in
 *  force on its date of its person's grade in force on that date; 0 when none: it is overdue past its due-by plus it); and
 *  how a claim's live figures round: the policy date (the policy-date setting, else the sheet's today; '' while
 *  loading) and the dates of the sheet's numbered claims (a correction rounds by the claim it corrects), and the
 *  rounding mode in force on a date (half-up or half-even; '' when none: the policy date rounds). */
export type Late = { pct: DatedValue; today: string; policy: string; numbered: Map<string, string>;
  grace: (claim: Claim) => number; rounding: DatedValue };

/** Whether a claim's live figures round half up: by the rounding mode in force on its date (a correction: the date
 *  of the claim it corrects) when there is one; else when it is dated before the policy date; else they round half
 *  to even. */
export function roundsUp(claim: Claim, late: Late): boolean {
  const date = claim.corrects !== null ? late.numbered.get(claim.corrects) ?? claim.date : claim.date;
  const mode = late.rounding(date);
  if (mode !== '') return mode === 'half-up';
  return late.policy !== '' && date < late.policy;
}

/** A claim's due: the one fixed when it was settled (its late charge frozen in), else the one its person's last
 *  statement bound it to as shown then (late charge and floor included; n/a included; an OPEN or PENDING one alike),
 *  else its floored figure (the greater of the sheet's floor in force on its date and its live figure on its amount at
 *  the rate) with the late charge worked out on that floored figure added. A correction (a negative amount) has no floor and no late charge. */
export function claimDue(claim: Claim, rate: string | undefined, floor: DatedValue, late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  if (claim.stated) return claim.statedDue ?? 'n/a';
  const correction = toCents(claim.amount) < 0n;
  const figure = dueOn(claim.amount, rate, roundsUp(claim, late));
  if (correction || figure === 'n/a') return figure;
  const base = floored(figure, floor(claim.date));
  return fromCents(toCents(base) + toCents(lateCharge(claim, base, late)));
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** The late charge on a claim whose figure is `figure`: for each full 30 days the claim is overdue (past its due-by
 *  plus its grace, on the sheet's today), the late percent in force on that boundary date (its due-by plus its grace
 *  plus 30, 60, 90 ... days) of it plus the charges of the boundaries before (compounding), each
 *  rounded like the claim's due, the charges added up (a boundary whose percent is 0.00 adds nothing); 0.00 when not
 *  overdue, or a correction (a negative amount), or its late charge was waived. */
export function lateCharge(claim: Claim, figure: string, late: Late): string {
  const grace = late.grace(claim);
  if (!overdue(claim, late.today, grace) || toCents(claim.amount) < 0n || claim.waived) return '0.00';
  const from = Date.parse(`${lateFrom(claim, grace)}T00:00:00Z`);
  const days = Math.round((Date.parse(`${late.today}T00:00:00Z`) - from) / DAY_MS);
  let total = 0n;
  for (let n = 1; n <= Math.floor(days / 30); n++) {
    const pct = late.pct(new Date(from + n * 30 * DAY_MS).toISOString().slice(0, 10));
    if (toCents(pct) !== 0n) total += toCents(dueOn(fromCents(toCents(figure) + total), pct, roundsUp(claim, late)));
  }
  return fromCents(total);
}

/** A due raised to the floor when below it; 'n/a' stays 'n/a'. */
export function floored(due: string, floor: string): string {
  return due === 'n/a' || toCents(due) >= toCents(floor) ? due : fromCents(toCents(floor));
}


/** A candidate rate's preview for a grade: of the OPEN and PENDING claims of people on the grade (the one in force on
 *  each claim's date), how many would show a different due (as shown on its row) if the candidate applied to the grade
 *  on every date, and the sum of the differences (new due minus current due; n/a counting as 0.00). A claim bound by a
 *  statement keeps its figure (see claimDue); a settled one is not counted. Nothing is saved. */
export function ratePreview(people: Person[], claims: Claim[], rates: Rate[], grade: string, pct: string,
    floor: DatedValue, late: Late): { count: number; diff: string } {
  const byId = new Map(people.map((p) => [p.id, p]));
  let count = 0;
  let diff = 0n;
  for (const c of claims) {
    const person = byId.get(c.personId);
    if (!person || (c.status !== 'OPEN' && c.status !== 'PENDING') || gradeOn(person, c.date) !== grade) continue;
    const was = claimDue(c, rateOn(rates, grade, c.date, c.id), floor, late);
    const now = claimDue(c, pct, floor, late);
    if (was === now) continue;
    count++;
    diff += (now === 'n/a' ? 0n : toCents(now)) - (was === 'n/a' ? 0n : toCents(was));
  }
  return { count, diff: fromCents(diff) };
}

/** A person's score: their settled figures (see personStatusSum, SETTLED) divided by 100, minus 5 for each of their
 *  overdue claims that count (see overdueDues), rounded half to even to a whole number (exact, in integers). */
export function personScore(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late): string {
  const own = claims.filter((c) => c.personId === person.id);
  const settled = toCents(personStatusSum(person, own, rates, today, floor, late, SETTLED_STATUSES));
  const overdueCount = BigInt(overdueDues(own, [person], rates, late, floor).length);
  const scaled = settled - overdueCount * 50000n; // in 1/10000 of a point
  const sign = scaled < 0n ? -1n : 1n;
  const abs = scaled * sign;
  let whole = abs / 10000n;
  const rest = abs % 10000n;
  if (rest > 5000n || (rest === 5000n && whole % 2n === 1n)) whole += 1n;
  return String(whole * sign);
}
