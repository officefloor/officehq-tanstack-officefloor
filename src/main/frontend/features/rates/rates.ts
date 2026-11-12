import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';
import { type Claim, counts, isBound, isCorrection } from '../claims/claims';
import { type Grades, gradeOn } from '../people/people';

/** A grade's rate: `pct` is a percent with two decimals; `from` (yyyy-MM-dd) is the date it applies
 *  from, null for from the beginning of time; `to` the date it applies through, null for no end. A
 *  grade may have several; one with no rate has none. `level` is the sheet it is on: 0 (or absent) for the
 *  sheet's own, 1 for its parent's, and so on up; a grade with no rate in force on a date on the sheet takes
 *  the first level up that has one. `version` is how many versions it has; `pct` is the newest's, the one in
 *  force. `forward` is set on a forward-only rate (saved with a from-date on or after the sheet's today): when
 *  it was saved, so it applies only to claims added after (see `ratesFor`); null or absent for one that applies
 *  to every claim. The rate in force (the rates panel and preview) is read without regard to it. */
export type Rate = { grade: string; pct: string; from: string | null; to: string | null; level?: number; version?: number;
  forward?: number | null };

/** The rates that apply to the claim: all but the forward-only ones saved after it was added. */
export function ratesFor(rates: Rate[] | undefined, claim: Claim): Rate[] | undefined {
  return rates?.filter((r) => r.forward == null || r.forward < claim.added);
}

/** The rates of each level, the sheet's own first. */
function levels(rates: Rate[] | undefined): Rate[][] {
  const by: Rate[][] = [];
  for (const r of rates ?? []) (by[r.level ?? 0] ??= []).push(r);
  return Array.from(by, (l) => l ?? []);
}

/** Under the sheet's key, so loading the sheet again re-reads its rates. */
export const ratesKey = (sheet: string) => [...sheetKey(sheet), 'rates'] as const;

export function useRates(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(ratesKey(sheet), asOf),
    queryFn: () => getJson<Rate[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/rates`, asOf)),
    enabled: sheet !== '',
  });
}

/** The grade's percent in force on the date (yyyy-MM-dd): the rate with the latest from-date on or
 *  before it on the sheet, else on the first parent sheet up that has one, or undefined when none of the grade's rates applies then, or that rate ended (its to-date) before it. */
export function rateOf(rates: Rate[] | undefined, grade: string, on: string | undefined): string | undefined {
  for (const level of levels(rates)) {
    const pct = ownRateOf(level, grade, on);
    if (pct !== undefined) return pct;
  }
  return undefined;
}

/** The grade's percent in force on the date among these rates of one sheet (see `rateOf`). */
function ownRateOf(rates: Rate[], grade: string, on: string | undefined): string | undefined {
  if (on === undefined) return undefined;
  let best: Rate | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade || (r.from !== null && r.from > on)) continue;
    if (best === undefined || (best.from ?? '') < (r.from ?? '')) best = r;
  }
  return best === undefined || (best.to !== null && best.to < on) ? undefined : best.pct;
}

/** Whether the grade's rate in force on the date ends before `today`: its to-date, or the day before the
 *  grade's next rate starts, whichever is first, is before it. The rate is that of the sheet that provides it
 *  (see `rateOf`). */
export function endsBefore(rates: Rate[] | undefined, grade: string, on: string, today: string): boolean {
  for (const level of levels(rates)) {
    if (ownRateOf(level, grade, on) !== undefined) return ownEndsBefore(level, grade, on, today);
    // a rate starting on a sheet nearer down the chain ends the one a sheet further up provides
    if (level.some((r) => r.grade === grade && r.from !== null && r.from > on && r.from <= today)) return true;
  }
  return false;
}

/** Whether the grade's rate in force on the date among these rates of one sheet ends before `today`. */
function ownEndsBefore(rates: Rate[], grade: string, on: string, today: string): boolean {
  let best: Rate | undefined;
  let next: string | undefined;
  for (const r of rates ?? []) {
    if (r.grade !== grade) continue;
    if (r.from !== null && r.from > on) {
      if (next === undefined || r.from < next) next = r.from;
    } else if (best === undefined || (best.from ?? '') < (r.from ?? '')) {
      best = r;
    }
  }
  if (best === undefined) return false;
  return (best.to !== null && best.to < today) || (next !== undefined && next <= today);
}

export const NA = 'n/a';

/** Whole hundredths of a two-decimal number, read exactly from its text. */
function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.trim().replace(/^-/, '').split('.');
  const n = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return value.trim().startsWith('-') ? -n : n;
}

/** Hundredths as text with two decimals. */
function format(cents: bigint): string {
  const neg = cents < 0n;
  const abs = neg ? -cents : cents;
  return `${neg ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** amount × pct ÷ 100, rounded half to even (half up when `halfUp`) to two decimals; n/a when there is no rate. */
export function due(amount: string, pct: string | undefined, halfUp = false): string {
  if (pct === undefined) return NA;
  // amount (hundredths) × pct (hundredths) is in units of 1/10000 cent-percent: ÷ 10000 gives cents.
  const product = hundredths(amount) * hundredths(pct);
  const abs = product < 0n ? -product : product;
  const rest = abs % 10000n;
  let cents = abs / 10000n;
  if (rest > 5000n || (rest === 5000n && (halfUp || cents % 2n === 1n))) cents += 1n;
  return format(product < 0n ? -cents : cents);
}

/** A live due: a figure raised to the sheet's floor (two decimals) when below it; n/a stays n/a. */
export function liveDue(figure: string, floor: string | undefined): string {
  if (figure === NA || floor === undefined || hundredths(figure) >= hundredths(floor)) return figure;
  return format(hundredths(floor));
}

/** The sum of these dues, skipping n/a; two decimals. */
export function dueTotal(dues: string[]): string {
  return format(dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0n));
}

/** `value` less `base`, two decimals, with a minus sign when lower. */
export function difference(value: string, base: string): string {
  return format(hundredths(value) - hundredths(base));
}

/** Shares of 100.00 split equally among `count` patrons in the order they were named: each rounded half to
 *  even to two decimals, with the remainder (100.00 less the shares added up) given to the first. */
export function patronShares(count: number): string[] {
  if (count === 0) return [];
  const n = BigInt(count);
  const rest = 10000n % n;
  let each = 10000n / n;
  if (rest * 2n > n || (rest * 2n === n && each % 2n === 1n)) each += 1n;
  const shares = Array.from({ length: count }, () => each);
  shares[0] += 10000n - each * n;
  return shares.map(format);
}

/** The sum of each figure (n/a counted as none) times its share ÷ 100, rounded half to even to two decimals. */
export function sharedTotal(parts: { figure: string; share: string }[]): string {
  // figure (hundredths) × share (hundredths) is in units of 1/10000 cent-percent: ÷ 10000 gives cents.
  const product = parts
    .filter((p) => p.figure !== NA)
    .reduce((sum, p) => sum + hundredths(p.figure) * hundredths(p.share), 0n);
  const abs = product < 0n ? -product : product;
  const rest = abs % 10000n;
  let cents = abs / 10000n;
  if (rest > 5000n || (rest === 5000n && cents % 2n === 1n)) cents += 1n;
  return format(product < 0n ? -cents : cents);
}

/** A sheet total of people's figures: their sum, skipping n/a, leaving out the single highest and the
 *  single lowest when three or more people have a figure. */
export function trimmedTotal(dues: string[]): string {
  const figures = dues.filter((d) => d !== NA).map(hundredths).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const kept = figures.length >= 3 ? figures.slice(1, -1) : figures;
  return format(kept.reduce((sum, d) => sum + d, 0n));
}

/** What a claim's late charge follows: the sheet's late percent (two decimals) in force on a date and the
 *  30-day boundaries (yyyy-MM-dd) the claim is overdue past; and whether the claim's live figures round half up (else half to even);
 *  and the date (yyyy-MM-dd) its overdue is counted from: its due-by plus any grace days of its person's grade. */
export type Late = { pct: ((date: string) => string) | undefined; boundaries: (claim: Claim) => string[]; halfUp?: (claim: Claim) => boolean;
  graceBy?: (claim: Claim) => string };

/** The sheet's floor (two decimals) in force on a date; undefined for none known. */
export type Floor = ((date: string) => string) | undefined;

/** The late charge on a figure, compounding: for every full 30 days overdue, the late percent in force on that
 *  boundary of the figure plus the charges of the boundaries before it, rounded like due, added up (a boundary at
 *  0.00 adds nothing). None on a correction, nor on a claim whose charge was waived. */
function lateCharge(claim: Claim, figure: string, late: Late | undefined): string | undefined {
  const boundaries = late?.pct === undefined || isCorrection(claim) || claim.waived ? [] : late.boundaries(claim);
  if (boundaries.length === 0 || figure === NA) return undefined;
  const halfUp = late?.halfUp?.(claim) ?? false;
  return format(boundaries.reduce(
    (sum, b) => sum + hundredths(due(format(hundredths(figure) + sum), late!.pct!(b), halfUp)), 0n));
}

/** Whether the claim keeps a figure whatever the rates: fixed when settled, or bound by a statement to a
 *  figure (bound to n/a keeps no figure). */
const fixed = (claim: Claim) => claim.due !== null || (isBound(claim) && claim.statedDue !== null);

/** A claim's due: the due fixed when it was settled (its late charge frozen in), else the due as shown its
 *  person's last statement bound it to (its late charge and floor in it), else its figure — its amount at the rate in force
 *  on the claim's date of its person's grade in force on that date — plus its late charge when overdue (worked out
 *  on that figure), and at least the sheet's floor in force on the claim's date, applied last.
 *  A correction's live due has no floor and it has no late charge.
 *  While bound by a statement, its late charge no longer follows the sheet's today. */
export function claimDue(
  claim: Claim,
  rates: Rate[] | undefined,
  grades: Grades,
  floor: Floor,
  late?: Late,
): string {
  if (claim.due !== null) return claim.due;
  if (isBound(claim)) return claim.statedDue ?? NA;
  const pct = rateOf(ratesFor(rates, claim), gradeOn(grades, claim.date), claim.date);
  const halfUp = late?.halfUp?.(claim) ?? false;
  const figure = due(claim.amount, pct, halfUp);
  const charge = lateCharge(claim, figure, late);
  const charged = charge === undefined ? figure : format(hundredths(figure) + hundredths(charge));
  return isCorrection(claim) ? charged : liveDue(charged, floor?.(claim.date));
}

/** A person's due-total: the sum of the figures of their claims that count, at most the sheet's cap
 *  (two decimals; null or undefined for none). n/a when their grade
 *  in force on the sheet's today has no rate then, unless a claim that counts keeps a fixed (settled or
 *  stated) figure, or a figure at another grade (the one in force on its date), or at a rate that ended before today;
 *  n/a is not capped. */
export function personDue(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
  cap?: string | null,
): string {
  const total = uncappedDue(claims, rates, grades, today, floor, late);
  return total === NA || cap == null || hundredths(total) <= hundredths(cap) ? total : format(hundredths(cap));
}

/** A person's excess: how far the sum of their figures is above the sheet's cap; 0.00 when it is not, or
 *  there is no cap, or their due-total is n/a. */
export function personExcess(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
  cap?: string | null,
): string {
  const total = uncappedDue(claims, rates, grades, today, floor, late);
  return total === NA || cap == null || hundredths(total) <= hundredths(cap)
    ? format(0n)
    : format(hundredths(total) - hundredths(cap));
}

/** A person's due-total before the cap (see `personDue`). */
function uncappedDue(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
): string {
  const mine = claims.filter(counts);
  const now = gradeOn(grades, today);
  const elsewhere = (c: Claim) => {
    const grade = gradeOn(grades, c.date);
    const theirs = ratesFor(rates, c);
    return rateOf(theirs, grade, c.date) !== undefined
      && (grade !== now || (today !== undefined && endsBefore(theirs, grade, c.date, today)));
  };
  if (rateOf(rates, now, today) === undefined && !mine.some((c) => fixed(c) || elsewhere(c))) {
    return NA;
  }
  return dueTotal(mine.map((c) => claimDue(c, rates, grades, floor, late)));
}

/** A person's settled-total and open-total: the sum of the dues (skipping n/a) of their claims that count
 *  that are SETTLED (at their frozen figures), and that are OPEN or PENDING. Together they make their
 *  due-total before the cap (0.00 each when it is n/a). */
export function personSplit(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
): { settled: string; open: string } {
  if (uncappedDue(claims, rates, grades, today, floor, late) === NA) return { settled: format(0n), open: format(0n) };
  const mine = claims.filter(counts);
  const total = (keep: (c: Claim) => boolean) =>
    dueTotal(mine.filter(keep).map((c) => claimDue(c, rates, grades, floor, late)));
  return { settled: total((c) => c.status === 'SETTLED'), open: total((c) => c.status !== 'SETTLED') };
}

/** A person's due on a tag: the sum of the dues (skipping n/a) of their claims that count that carry it, or
 *  all of them when the person (whose tags are `personTags`) carries it, each once; 0.00 when their due-total is n/a. */
export function personTagged(
  claims: Claim[],
  tag: string,
  personTags: string[],
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
): string {
  return personDueWhere(claims, (c) => personTags.includes(tag) || c.tags.includes(tag), rates, grades, today, floor, late);
}

/** A person's due in a calendar month (yyyy-mm): the sum of the dues (skipping n/a) of their claims that
 *  count dated in it; 0.00 when their due-total is n/a. */
export function personMonthDue(
  claims: Claim[],
  month: string,
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
): string {
  return personDueWhere(claims, (c) => c.date.slice(0, 7) === month, rates, grades, today, floor, late);
}

/** The sum of the dues (skipping n/a) of a person's claims that count that `keep` picks; 0.00 when their
 *  due-total is n/a. */
function personDueWhere(
  claims: Claim[],
  keep: (c: Claim) => boolean,
  rates: Rate[] | undefined,
  grades: Grades,
  today: string | undefined,
  floor: Floor,
  late?: Late,
): string {
  if (uncappedDue(claims, rates, grades, today, floor, late) === NA) return format(0n);
  return dueTotal(claims.filter((c) => counts(c) && keep(c)).map((c) => claimDue(c, rates, grades, floor, late)));
}

/** A preview of a candidate percent for a grade: of these people's OPEN and PENDING claims whose person
 *  was on the grade on the claim's date, how many would show a different due were the candidate the
 *  grade's only rate (from the beginning of time, no end), and the sum of new due less current due (n/a
 *  as 0.00). A claim bound by a statement or settled keeps its figure. Nothing is saved. */
export function ratePreview(
  people: { grades: Grades; claims: Claim[]; late?: Late }[],
  rates: Rate[] | undefined,
  grade: string,
  pct: string,
  floor: Floor,
): { count: number; diff: string } {
  const candidate: Rate[] = [...(rates ?? []).filter((r) => r.grade !== grade || (r.level ?? 0) > 0), { grade, pct, from: null, to: null }];
  const figure = (d: string) => (d === NA ? 0n : hundredths(d));
  let count = 0;
  let diff = 0n;
  for (const p of people) {
    for (const c of p.claims) {
      if (c.status !== 'OPEN' && c.status !== 'PENDING') continue;
      if (gradeOn(p.grades, c.date) !== grade) continue;
      const now = claimDue(c, rates, p.grades, floor, p.late);
      const then = claimDue(c, candidate, p.grades, floor, p.late);
      if (now === then) continue;
      count += 1;
      diff += figure(then) - figure(now);
    }
  }
  return { count, diff: format(diff) };
}
