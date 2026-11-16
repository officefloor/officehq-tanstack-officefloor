import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { nowKey, useReadNow } from '../../query/readNow';
import { counts, isOpen, type Claim } from '../claims/claims';
import { lateBoundaries, roundsHalfUp, type Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';
import type { Dated } from '../settings/settings';

/** A grade's rate on the sheet: `rate` is a percent as text with two decimals (never a float), in force from
 *  `from` (yyyy-MM-dd; null: from the beginning of time) through `to` (yyyy-MM-dd; null: no end). `level` is how far
 *  up the parent chain its sheet is: 0 the sheet's own, 1 its parent's, up to 3 (absent: 0). `version` is how many
 *  versions the grade and from-date has had, this being the newest (the one in force). */
export type Rate = {
  sheetId: string;
  grade: string;
  rate: string;
  from: string | null;
  to: string | null;
  version: number;
  level?: number;
  /** Set on a forward-only rate (saved with a from-date on or after the sheet's today): it applies only to claims whose
   *  seq is above it (see claimRate); null or absent when it applies to every claim. */
  after?: number | null;
  /** The versions it replaced, newest first: each still applies to a claim the newer ones do not (see claimRate). */
  older?: { rate: string; to: string | null; version: number; after?: number | null }[];
};

export const ratesKey = (sheetId: string) => ['sheet', sheetId, 'rates'];

/** The sheet's rates, by grade then from-date, then those of its parent, the parent's parent ... (see Rate.level). */
export function useRates(sheetId: string) {
  const now = useReadNow();
  return useQuery({
    queryKey: nowKey(ratesKey(sheetId), now),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, now),
  });
}

/** The sheet's own rates: those not inherited from a parent sheet. */
export function ownRates(rates: Rate[] | undefined): Rate[] {
  return (rates ?? []).filter((r) => (r.level ?? 0) === 0);
}

/** The grade's rate in force on a date (yyyy-MM-dd) on the sheet (see levelRate), or, when it has none, on its parent,
 *  then the parent's parent, up the levels the rates carry; undefined when none of them has one. */
export function rateFor(rates: Rate[] | undefined, grade: string, on: string): string | undefined {
  const levels = [...new Set((rates ?? []).map((r) => r.level ?? 0))].sort((a, b) => a - b);
  for (const level of levels) {
    const found = levelRate((rates ?? []).filter((r) => (r.level ?? 0) === level), grade, on);
    if (found !== undefined) return found;
  }
  return undefined;
}

/** The grade's rate in force on a date among one sheet's rates: the one with the latest from-date on or before it, a
 *  rate with no from-date applying from the beginning of time; undefined when none applies, or that rate's to-date is
 *  before the date. */
function levelRate(rates: Rate[], grade: string, on: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found === undefined || (found.to !== null && found.to < on) ? undefined : found.rate;
}

/** Whether a rate (or an older version of one) applies to a claim: always, unless it is forward-only and the claim was
 *  added before it was saved. */
function appliesTo(r: { after?: number | null }, claim: Claim): boolean {
  return r.after === null || r.after === undefined || claim.seq === undefined || claim.seq > r.after;
}

/** The grade's rate that applies to a claim (on its date), as the server works it out: as rateFor, but a forward-only
 *  rate saved after the claim was added is passed over for the newest older version of it that applies, else for
 *  the rate with the next latest from-date. The rates panel, preview and calendar show rateFor, the rate in force. */
export function claimRate(rates: Rate[] | undefined, grade: string, claim: Claim): string | undefined {
  const levels = [...new Set((rates ?? []).map((r) => r.level ?? 0))].sort((a, b) => a - b);
  for (const level of levels) {
    const own: Rate[] = [];
    for (const r of rates ?? []) {
      if ((r.level ?? 0) !== level || r.grade !== grade) continue;
      const version = [r, ...(r.older ?? []).map((o) => ({ ...r, ...o }))].find((v) => appliesTo(v, claim));
      if (version !== undefined) own.push(version);
    }
    const found = levelRate(own, grade, claim.date);
    if (found !== undefined) return found;
  }
  return undefined;
}

/** A two-decimal number ("12.34") in hundredths, exactly. */
function hundredths(value: string): bigint {
  const t = value.trim();
  const negative = t.startsWith('-');
  const [whole, frac = ''] = (negative ? t.slice(1) : t).split('.');
  const h = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -h : h;
}

/** Hundredths shown with two decimals. */
function shown(h: bigint): string {
  const sign = h < 0n ? '-' : '';
  const a = h < 0n ? -h : h;
  return `${sign}${a / 100n}.${String(a % 100n).padStart(2, '0')}`;
}

/** What is due on an amount at a rate: amount × rate / 100, rounded to two decimals (half to even, or half up when
 *  `halfUp`); 'n/a' when there is no rate. */
export function due(amount: string, rate: string | undefined, halfUp = false): string {
  if (rate === undefined) return 'n/a';
  return shown(percentOf(amount, rate, 1, halfUp));
}

/** `times` × value × pct / 100 in hundredths, rounded to two decimals: half to even, or half up (away from zero)
 *  when `halfUp`. */
function percentOf(value: string, pct: string, times: number, halfUp: boolean): bigint {
  const p = hundredths(value) * hundredths(pct) * BigInt(times); // in 1/10000 of a hundredth
  const a = p < 0n ? -p : p;
  const q = a / 10000n;
  const rem = a % 10000n;
  const r = rem > 5000n || (rem === 5000n && (halfUp || q % 2n === 1n)) ? q + 1n : q;
  return p < 0n ? -r : r;
}

/** A claim's due: the figure fixed when it was settled, otherwise the due as shown (late charge included) its person's
 *  last statement bound it to (PENDING included), otherwise its amount at the rate in force on its date. Given the
 *  sheet's late charge, an overdue claim's figure gains, for every full 30 days overdue, the late percent in force on
 *  that boundary (its due-by plus 30, 60, 90 ... days) of it plus the charges of the boundaries before it
 *  (compounding), each rounded like the claim and added up (frozen in when settled). Last, the live due is at least
 *  the sheet's floor in force on its date (a correction has no floor). Both round by the sheet's policy date (see Late). */
export function claimDue(claim: Claim, rate: string | undefined, floor: Dated, late?: Late): string {
  if (claim.due !== null) return claim.due;
  if (claim.stated) return claim.statedDue ?? 'n/a';
  const halfUp = roundsHalfUp(claim, late);
  const figure = due(claim.amount, rate, halfUp);
  if (figure === 'n/a') return figure;
  const boundaries = late === undefined ? [] : lateBoundaries(claim, late);
  const charged = boundaries.reduce((t, b) => t + percentOf(shown(t), late!.pct(b), 1, halfUp), hundredths(figure));
  const f = hundredths(floor(claim.date));
  return shown(charged < f && hundredths(claim.amount) >= 0n ? f : charged);
}

/** The sum of a person's figures: the figures of their claims that count (each at the rate, of the grade in force,
 *  on its date, with its late charge) added up; 'n/a' when their grade has no rate today and none of those claims has
 *  a figure. */
function personFigures(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated): string {
  const counted = claims.filter(counts);
  const figures = counted.map((c) => claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late));
  if (rateFor(rates, gradeOn(person, late.today), late.today) === undefined && figures.every((f) => f === 'n/a')) return 'n/a';
  return sum(figures);
}

/** A person's settled-total and open-total: the figures (see personFigures) of their SETTLED claims that count
 *  (frozen when settled, corrections included), and of their OPEN and PENDING ones, each added up, skipping n/a.
 *  Together they make the sum of their figures, before the cap. */
export function personSplit(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated): { settled: string; open: string } {
  const figure = (c: Claim) => claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late);
  const counted = claims.filter(counts);
  return {
    settled: sum(counted.filter((c) => c.status === 'SETTLED').map(figure)),
    open: sum(counted.filter((c) => c.status !== 'SETTLED').map(figure)),
  };
}

/** A person's due-total: the sum of their figures (see personFigures), at most the sheet's cap (null: none); 'n/a'
 *  is not capped. */
export function personDue(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated, cap: string | null): string {
  const total = personFigures(claims, rates, person, late, floor);
  return total !== 'n/a' && cap !== null && hundredths(total) > hundredths(cap) ? shown(hundredths(cap)) : total;
}

/** A person's excess: how far the sum of their figures is above the sheet's cap; 0.00 when it is not (or with no
 *  cap, or an n/a due-total). */
export function personExcess(claims: Claim[], rates: Rate[] | undefined, person: Person, late: Late, floor: Dated, cap: string | null): string {
  const total = personFigures(claims, rates, person, late, floor);
  return total !== 'n/a' && cap !== null && hundredths(total) > hundredths(cap)
    ? shown(hundredths(total) - hundredths(cap))
    : '0.00';
}

/** A candidate rate: a percent 0.00 or more with up to two decimals (as the rate form takes it). */
export const RATE_PATTERN = /^\d{1,5}(\.\d{1,2})?$/;

/** What a candidate rate would change if it applied to the grade on every date: of the OPEN and PENDING claims whose
 *  person is on the grade on the claim's date, how many would show a different due, and the sum of the differences
 *  (new due minus current due; an n/a due counts as 0.00). A claim bound by a statement keeps its due, so never
 *  changes; settled claims are not looked at. */
export function ratePreview(
  claims: Claim[],
  people: Person[],
  rates: Rate[] | undefined,
  grade: string,
  pct: string,
  late: Late,
  floor: Dated,
): { count: number; diff: string } {
  const byId = new Map(people.map((p) => [p.id, p]));
  const value = (d: string) => (d === 'n/a' ? 0n : hundredths(d));
  let count = 0;
  let diff = 0n;
  for (const c of claims) {
    const person = byId.get(c.personId);
    if (!isOpen(c) || person === undefined || gradeOn(person, c.date) !== grade) continue;
    const now = claimDue(c, claimRate(rates, grade, c), floor, late);
    const then = claimDue(c, pct, floor, late);
    if (now === then) continue;
    count++;
    diff += value(then) - value(now);
  }
  return { count, diff: shown(diff) };
}

/** 100.00 split equally among `n` in order: each share rounded half to even to two decimals, the remainder (100.00
 *  minus the shares added up) given to the first. */
export function equalShares(n: number): string[] {
  if (n === 0) return [];
  const N = BigInt(n);
  const q = 10000n / N;
  const rem = 10000n % N;
  const each = rem * 2n > N || (rem * 2n === N && q % 2n === 1n) ? q + 1n : q;
  return Array.from({ length: n }, (_, i) => shown(i === 0 ? 10000n - each * (N - 1n) : each));
}

/** The sum of each two-decimal value × its percent / 100, rounded once to two decimals, half to even. */
export function sharedSum(parts: { value: string; pct: string }[]): string {
  const p = parts.reduce((s, x) => s + hundredths(x.value) * hundredths(x.pct), 0n); // in 1/10000 of a hundredth
  const a = p < 0n ? -p : p;
  const q = a / 10000n;
  const rem = a % 10000n;
  const r = rem > 5000n || (rem === 5000n && q % 2n === 1n) ? q + 1n : q;
  return shown(p < 0n ? -r : r);
}

/** Two-decimal values added up, skipping 'n/a'. */
export function sum(values: string[]): string {
  return shown(values.filter((v) => v !== 'n/a').reduce((s, v) => s + hundredths(v), 0n));
}

/** One two-decimal value minus another, two decimals, a minus sign when below zero. */
export function difference(value: string, less: string): string {
  return shown(hundredths(value) - hundredths(less));
}

/** Two-decimal values added up, skipping 'n/a', leaving out the single highest and the single lowest when three
 *  or more are left. */
export function trimmedSum(values: string[]): string {
  const figures = values.filter((v) => v !== 'n/a').map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return shown(kept.reduce((s, v) => s + v, 0n));
}
