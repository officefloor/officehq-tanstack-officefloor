import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { counts, dueBy, live, overdue, type Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import type { DatedValue } from '../settings/settings';

/** A grade's rate on the sheet: a percent with two decimals, in force from its from-date (yyyy-mm-dd, or
 *  null: from the beginning of time) through its to-date (null: with no end). A grade may have several, one
 *  per from-date; none when it has no rate. */
export type Rate = { grade: string; rate: string; from: string | null; to: string | null };

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'] as const;

/** The sheet's rates, every grade's every from-date. */
export function useRates(sheetId: string) {
  return useQuery({
    queryKey: ratesKey(sheetId),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`),
  });
}

/** A grade's rate in force on a date (yyyy-mm-dd): the one whose from-date is the latest on or before
 *  it, a rate with no from-date applying from the beginning of time; undefined when none applies, or that
 *  one's to-date is before the date. */
export function rateOn(rates: Rate[], grade: string, date: string): string | undefined {
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
function toCents(amount: string): bigint {
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
export function personDue(claims: Claim[], rateOnDate: (date: string) => string | undefined,
    rateToday: string | undefined, floor: DatedValue, late: Late): string {
  const counted = claims.filter(counts);
  const dues = counted.map((c) => claimDue(c, rateOnDate(c.date), floor, late));
  if (rateToday === undefined && dues.every((d) => d === 'n/a')) return 'n/a';
  return sumDue(dues);
}

/** A person's figures added up, before the cap: their live claims among these (see live) through personDue, at
 *  the grade in force on each claim's date and on the sheet's today. */
export function personDueSum(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late): string {
  return personDue(live(claims.filter((c) => c.personId === person.id), [person], today),
    (d) => rateOn(rates, gradeOn(person, d), d), rateOn(rates, gradeOn(person, today), today), floor, late);
}

/** A person's dues of one kind: the due (as shown on its row; a SETTLED one's frozen) of each of their claims that
 *  count in the live totals (see live) and whose status is one of `statuses`, added up, skipping n/a. Their
 *  settled-total (SETTLED) and open-total (OPEN, PENDING) add up to their figures before the cap (see personDueSum). */
export function personStatusSum(person: Person, claims: Claim[], rates: Rate[], today: string, floor: DatedValue,
    late: Late, statuses: Claim['status'][]): string {
  return sumDue(live(claims.filter((c) => c.personId === person.id), [person], today)
    .filter((c) => statuses.includes(c.status))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late)));
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
    .map((c) => claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId), c.date), c.date), floor, late)));
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
    .filter((c) => overdue(c, late.today))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId), c.date), c.date), floor, late));
}

/** What the late charge follows: the sheet's late percent by date (two decimals; a claim gains the one in force
 *  on each 30-day boundary past its due-by) and its today; and
 *  how a claim's live figures round: the policy date (the policy-date setting, else the sheet's today; '' while
 *  loading) and the dates of the sheet's numbered claims (a correction rounds by the claim it corrects). */
export type Late = { pct: DatedValue; today: string; policy: string; numbered: Map<string, string> };

/** Whether a claim's live figures round half up: it (a correction: the claim it corrects) is dated before the
 *  policy date; else they round half to even. */
export function roundsUp(claim: Claim, late: Late): boolean {
  const date = claim.corrects !== null ? late.numbered.get(claim.corrects) ?? claim.date : claim.date;
  return late.policy !== '' && date < late.policy;
}

/** A claim's due: the one fixed when it was settled (its late charge frozen in), else the greater of the sheet's floor
 *  in force on its date and its figure — the one its person's last statement bound it to, n/a included (not while it
 *  is PENDING: then it is live), else live on its amount at the rate — with the late charge on that figure added. A
 *  correction (a negative amount) has no floor and no late charge. */
export function claimDue(claim: Claim, rate: string | undefined, floor: DatedValue, late: Late): string {
  if (claim.settledDue !== null) return claim.settledDue;
  const correction = toCents(claim.amount) < 0n;
  const live = dueOn(claim.amount, rate, roundsUp(claim, late));
  const bound = claim.status === 'PENDING' || !claim.stated ? null : claim.statedDue ?? 'n/a';
  const figure = bound ?? live;
  if (correction || figure === 'n/a') return figure;
  return floored(fromCents(toCents(figure) + toCents(lateCharge(claim, figure, late))), floor(claim.date));
}

const DAY_MS = 24 * 60 * 60 * 1000;

/** The late charge on a claim whose figure is `figure`: for each full 30 days the claim is overdue (past its due-by, on
 *  the sheet's today), the late percent in force on that boundary date (its due-by plus 30, 60, 90 ... days) of it,
 *  rounded like the claim's due, the charges added up (a boundary whose percent is 0.00 adds nothing); 0.00 when not
 *  overdue, or a correction (a negative amount). */
export function lateCharge(claim: Claim, figure: string, late: Late): string {
  if (!overdue(claim, late.today) || toCents(claim.amount) < 0n) return '0.00';
  const from = Date.parse(`${dueBy(claim)}T00:00:00Z`);
  const days = Math.round((Date.parse(`${late.today}T00:00:00Z`) - from) / DAY_MS);
  let total = 0n;
  for (let n = 1; n <= Math.floor(days / 30); n++) {
    const pct = late.pct(new Date(from + n * 30 * DAY_MS).toISOString().slice(0, 10));
    if (toCents(pct) !== 0n) total += toCents(dueOn(figure, pct, roundsUp(claim, late)));
  }
  return fromCents(total);
}

/** A due raised to the floor when below it; 'n/a' stays 'n/a'. */
export function floored(due: string, floor: string): string {
  return due === 'n/a' || toCents(due) >= toCents(floor) ? due : fromCents(toCents(floor));
}

