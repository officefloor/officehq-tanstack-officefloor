import { useQuery } from '@tanstack/react-query';
import { useReadScope } from '../../query/live';
import { gradeOn, setAsideOn, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { dueCents, hundredths, claimRateOf, rateOf, twoDecimals, useRates, type Rate } from '../rates/rates';
import { useSheet, useSheetToday } from '../sheet/sheet';
import { useCapCents, useFloorCents, useGraceDays, useLatePctCents, usePolicyDate, useRoundingMode, type DatedCents, type RoundingModeOn } from '../settings/settings';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN/PENDING -> VOID. A clerk's settle of a
 *  due above the clerk limit makes it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN).
 *  A DRAFT may be DELETED: it counts nowhere, and is restored to a DRAFT only on the sheet's today it was deleted.
 *  A DRAFT whose due is above the review threshold goes to REVIEW when opened: it counts nowhere until a steward's
 *  review opens it. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';

/** Every status the screen counts, in the order it lists them (a DELETED claim counts nowhere). */
export const CLAIM_STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/** How many of the claims have the status (every claim, whoever's). */
export function statusCount(claims: Claim[], status: ClaimStatus): number {
  return claims.filter((c) => c.status === status).length;
}

/** A REVIEW claim's review day: the sheet's today when it last went to REVIEW, as its status log records it
 *  (undefined when the log has none). */
export function reviewDay(claim: Claim): string | undefined {
  let on: string | undefined;
  for (const line of claim.log) if (line.status === 'REVIEW') on = line.on;
  return on;
}

/** Whether a claim is late for review: REVIEW, with its review day plus the review SLA (null: none) before the
 *  sheet's today (not yet known: no). */
export function reviewLate(claim: Claim, sla: number | null, today: string | undefined): boolean {
  if (claim.status !== 'REVIEW' || sla === null || today === undefined) return false;
  const day = reviewDay(claim);
  return day !== undefined && plusDays(day, sla) < today;
}

/** A status change a claim's row offers; the server refuses one its status does not allow. */
export type ClaimStatusChange = 'open' | 'settle' | 'void' | 'approve' | 'reject' | 'delete' | 'restore' | 'review';

/** Whether a claim is still open: OPEN, or PENDING (handled like OPEN). */
export function isOpen(claim: Claim): boolean {
  return claim.status === 'OPEN' || claim.status === 'PENDING';
}

/** Whether a claim's statement binds its figure: it is stated (a PENDING claim too, like any that counts). */
export function bound(claim: Claim): boolean {
  return claim.stated;
}

/** A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its number on
 * the sheet (given when it is opened; null while a draft; kept once voided), and its due fixed when it was
 * settled (two decimals; null while live: until settled, and when its grade had no rate then), and the
 * user who opened it (null until opened; that user may not settle it), and whether its person's last statement
 * bound it, with the figure it bound (null when its grade had no rate then), and the number the claim it
 * corrects shows (null unless it is a correction: a negative amount), and the date it rounds on (its own, or for a
 * correction the date of the claim it corrects), and its number within its period (<period>-<n>, or X-<n> in no
 * period) when it was opened while the sheet numbered by period, shown in place of its number (null otherwise), and
 * its due-by (yyyy-MM-dd), fixed when it was added as the terms-th business day after its date (skipping weekends and holidays), from the terms of its person's grade in force on its date (worked out again with those terms when a holiday on or before it is added, unless SETTLED or VOID), and
 * its status log, oldest first, and the date it was last deferred until (null when never), with whether it is
 * deferred now (OPEN, or VOID when voided while deferred, with that date after the sheet's today: it counts in no total, is not overdue and cannot be
 * settled), and the number the claim it was split off shows (null unless it was split off one). */
export type Claim = {
  id: number;
  person: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  settledDue: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  roundsOn: string;
  periodNumber: string | null;
  dueBy: string;
  log: ClaimLogLine[];
  deferredUntil: string | null;
  deferred: boolean;
  /** How many proofs the claim has: one above the sheet's proof threshold needs one to be settled or approved. */
  proofs: number;
  splitFrom: string | null;
  /** The user whose open editor locks the claim against everyone else's edit button (null when none does). */
  lockedBy: string | null;
  /** Whether it was added while the period its due-by falls in was soft-closed. */
  lateEntry: boolean;
  /** The sheet's today it was last reminded on (null when never): an OPEN or PENDING claim is reminded once per day. */
  remindedOn: string | null;
  /** Its external reference, trimmed (null for none): no two claims on the sheet share one, ignoring case. */
  ref: string | null;
  /** Its tags: each trimmed and lower-cased, repeats dropped, sorted (a claim split off another carries its tags). */
  tags: string[];
  /** Every tag it has ever carried, sorted: the sheet lists a due-total for each tag any of its claims ever carried. */
  tagsEver: string[];
  /** The number of the bulk settle batch that last settled it (null when settled one at a time, or never). */
  batch: number | null;
  /** Its comments, in the order written. */
  comments: ClaimComment[];
  /** Its lines, in the order added: with any, its amount is their quantities times unit amounts added up. */
  lines: ClaimLine[];
  /** When it was added (the claim sequence's value; null when kept from before it was recorded): it sees only the
   *  forward-only rates saved before it (see {@link claimRateOf}). */
  added: number | null;
};

/** One line of a claim: its label, quantity (a whole number) and unit amount (two decimals). */
export type ClaimLine = { label: string; qty: number; unit: string };

/** One comment on a claim: the user who wrote it, the sheet's today then (yyyy-MM-dd), and its text. */
export type ClaimComment = { by: string; on: string; text: string };

/** One line of a claim's status log: the status it took, by whom, on the sheet's today then (yyyy-MM-dd), and the
 * claim's due as computed at that moment (two decimals, or n/a). */
export type ClaimLogLine = { status: ClaimStatus; by: string; on: string; due: string };

export const claimsKey = (sheet: string) => ['claims', sheet] as const;

export const claimsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  const read = useReadScope(claimsKey(sheet));
  return useQuery({
    queryKey: read.key,
    queryFn: () => read.get<Claim[]>(claimsPath(sheet)),
  });
}

/** The claims that count toward claimed, due-total and every total: the OPEN, PENDING and SETTLED ones, but not
 *  the deferred ones, nor the OPEN or PENDING ones dated within a hold of their person or whose person's retirement
 *  date is reached, unless bound by a statement. */
export function counted(claims: Claim[], people: Person[]): Claim[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return claims.filter((c) => {
    if (c.deferred) return false;
    if (c.status === 'SETTLED') return true;
    if (!isOpen(c)) return false;
    const person = byId.get(c.person);
    return bound(c) || person === undefined || !setAsideOn(person, c.date);
  });
}

/** The status a claim had at the end of the day (yyyy-MM-dd), as its status log records it (the last line on or before
 *  it); undefined when it was not yet added then. A claim kept from before the log (no lines) has its status now. */
export function statusOn(claim: Claim, end: string): ClaimStatus | undefined {
  if (claim.log.length === 0) return claim.status;
  let then: ClaimStatus | undefined;
  for (const line of claim.log) if (line.on <= end) then = line.status;
  return then;
}

/** Whether a claim counts toward a period ending on the day (yyyy-MM-dd): by the status it had at the end of it
 *  (OPEN, PENDING or SETTLED; not yet added: no), unless deferred now, or open now (as {@link counted}), unbound and
 *  dated within a hold of its person or its person's retirement date is reached. */
export function countedOn(claim: Claim, person: Person | undefined, end: string): boolean {
  const then = statusOn(claim, end);
  if (claim.deferred || then === undefined) return false;
  if (then === 'SETTLED') return true;
  if (then !== 'OPEN' && then !== 'PENDING') return false;
  return !isOpen(claim) || bound(claim) || person === undefined || !setAsideOn(person, claim.date);
}

/** The sum of the claims' amounts with two decimals, added in whole cents so no float error shows, leaving out the
 *  corrections (claims that correct another). */
export function claimed(claims: Claim[]): string {
  return twoDecimals(claims.reduce((sum, c) => (correction(c) ? sum : sum + hundredths(c.amount)), 0n));
}

/** Whether a claim is a correction: a negative amount, correcting the settled claim it names by number. */
export function correction(claim: Claim): boolean {
  return claim.amount.startsWith('-');
}

/** Whether a claim keeps its figure whatever the rates: fixed when settled, or bound by a statement (a claim
 *  settled or bound when its grade had no rate keeps none). */
export function fixed(claim: Claim): boolean {
  return claim.settledDue !== null || (bound(claim) && claim.statedDue !== null);
}

/** What makes an overdue claim's late charge: the sheet's late percent (in hundredths) by date, the one in force on
 *  each 30-day boundary after the claim's due-by, and its today.
 *  The charge is never bound by a statement: it follows the sheet's today. It also carries the sheet's policy date
 *  (yyyy-MM-dd; undefined: its today), which decides how a claim's live figures round. And the date a claim's
 *  overdue days are counted from (yyyy-MM-dd): its due-by plus the grace of its person's grade in force on its date,
 *  where that grade has one in force on that date. And the sheet's rounding mode by date, which, where one is in force
 *  on the date a claim rounds on, decides its rounding in place of the policy date. */
export type Late = { pct: DatedCents; today: string | undefined; policy: string | undefined; graceBy: (claim: Claim) => string;
  rounding?: RoundingModeOn };

/** The sheet's late percent, today, policy date, graces and rounding mode. */
export function useLate(sheet: string): Late {
  const today = useSheetToday(sheet);
  const policy = usePolicyDate(sheet);
  const grace = useGraceDays(sheet);
  const rounding = useRoundingMode(sheet);
  const { data: people } = usePeople(sheet);
  const graceBy = (claim: Claim) => {
    const person = people?.find((p) => p.id === claim.person);
    const days = person === undefined ? 0 : grace(gradeOn(person, claim.date), claim.date);
    return days === 0 ? claim.dueBy : plusDays(claim.dueBy, days);
  };
  return { pct: useLatePctCents(sheet), today, policy: policy === '' ? today : policy, graceBy, rounding };
}

/** Whether a claim's live figures round half up: by the rounding mode in force on the date it rounds on (its own, or
 *  for a correction the date of the claim it corrects) where there is one; else when that date is before the policy
 *  date; on or after it they round half to even. */
export function roundsHalfUp(claim: Claim, late: Late): boolean {
  const mode = late.rounding?.(claim.roundsOn) ?? '';
  if (mode === 'half-up') return true;
  if (mode === 'half-even') return false;
  return late.policy !== undefined && claim.roundsOn < late.policy;
}

/** A claim's figure: the one fixed when it was settled, else the rate figure its statement bound, else its amount at the
 *  grade's rate in force on its date (in hundredths); the floor is applied last, in {@link claimDueCents}. */
export function claimFigureCents(claim: Claim, pct: string | undefined, floor: DatedCents, halfUp = false): bigint | undefined {
  if (claim.settledDue !== null) return hundredths(claim.settledDue);
  if (bound(claim)) return claim.statedDue === null ? undefined : hundredths(claim.statedDue);
  return dueCents(claim.amount, pct, halfUp);
}

/** The whole days a claim is overdue: from its due-by plus its grace to the sheet's today (0 when it is not overdue). */
function daysOverdue(claim: Claim, late: Late): number {
  if (!overdue(claim, late)) return 0;
  return Math.round((Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${late.graceBy(claim)}T00:00:00Z`)) / 86_400_000);
}

/** The day (yyyy-MM-dd) the given number of days after a day. */
function plusDays(day: string, days: number): string {
  return new Date(Date.parse(`${day}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/** A claim's late charge on its figure (in hundredths), compounding: for each full 30 days it is overdue, the late
 *  percent in force on that boundary (its due-by plus its grace plus 30, 60, 90 ... days) of the figure plus the
 *  charges of the boundaries before it, rounded as the claim's live figures
 *  are (see {@link roundsHalfUp}), the charges added up; a correction has none. */
export function lateCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (correction(claim)) return 0n;
  const periods = Math.floor(daysOverdue(claim, late) / 30);
  let total = 0n;
  for (let k = 1; k <= periods; k++) {
    const pct = late.pct(plusDays(late.graceBy(claim), 30 * k));
    if (pct === 0n) continue;
    const product = (figure + total) * pct;
    const quotient = product / 10000n;
    const remainder = product % 10000n;
    const up = remainder > 5000n || (remainder === 5000n && (roundsHalfUp(claim, late) || quotient % 2n === 1n));
    total += up ? quotient + 1n : quotient;
  }
  return total;
}

/** A claim's due: the one fixed when settled; else the greater of the sheet's floor in force on its date and its
 *  figure plus, while it is overdue, its late charge on that figure (a correction has no floor and no charge). */
export function claimDueCents(claim: Claim, pct: string | undefined, floor: DatedCents, late: Late): bigint | undefined {
  const figure = claimFigureCents(claim, pct, floor, roundsHalfUp(claim, late));
  if (figure === undefined || claim.settledDue !== null || correction(claim)) return figure;
  const charged = figure + lateCents(claim, figure, late);
  const least = floor(claim.date);
  return charged >= least ? charged : least;
}


/** A person's due-total: the sum of their counted claims' rounded figures (the claims of others are skipped). n/a (undefined) when their grade
 *  has no rate on the sheet's today, unless a claim has a figure apart from it (settled, stated, at another grade
 *  in force on its date, or from a rate that ended by today on a claim not dated after it): then the figures there are.
 *  With a cap, a due-total above it is the cap (an n/a one is not capped). */
export function personDueCents(
  claims: Claim[],
  rates: Rate[],
  person: Person,
  today: string | undefined,
  floor: DatedCents,
  late: Late,
  cap?: bigint,
): bigint | undefined {
  const sum = uncappedPersonDueCents(claims, rates, person, today, floor, late);
  return sum !== undefined && cap !== undefined && sum > cap ? cap : sum;
}

/** A person's due-total before the cap: the sum of their figures (see {@link personDueCents}). */
export function uncappedPersonDueCents(
  claims: Claim[],
  rates: Rate[],
  person: Person,
  today: string | undefined,
  floor: DatedCents,
  late: Late,
): bigint | undefined {
  const mine = counted(claims.filter((c) => c.person === person.id), [person]);
  // A claim has a figure apart from today's grade when it keeps one, or when another grade in force on its date gives one.
  const apart = (c: Claim) => {
    const then = gradeOn(person, c.date);
    return fixed(c) || (then !== undefined && then !== person.grade && claimFigureCents(c, claimRateOf(rates, then, c), floor) !== undefined)
      || (then !== undefined && today !== undefined && c.date <= today && claimFigureCents(c, claimRateOf(rates, then, c), floor) !== undefined);
  };
  if (rateOf(rates, person.grade, today) === undefined && !mine.some(apart)) return undefined;
  return mine.reduce((sum, c) => sum + (claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n), 0n);
}

/** A person's settled-total and open-total (in hundredths): the dues of their SETTLED claims that count (at their
 *  frozen figures, corrections included) and of their OPEN and PENDING ones that count (see {@link counted}). Together
 *  they make the due-total before the cap; a claim with no due adds nothing. */
export function splitDueCents(
  claims: Claim[],
  rates: Rate[],
  person: Person,
  floor: DatedCents,
  late: Late,
): { settled: bigint; open: bigint } {
  const split = { settled: 0n, open: 0n };
  for (const c of counted(claims.filter((c) => c.person === person.id), [person])) {
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    if (c.status === 'SETTLED') split.settled += due;
    else if (isOpen(c)) split.open += due;
  }
  return split;
}

/** A person's recent settled figure: the sum of their SETTLED claims' frozen figures dated within the 90 days up to
 *  and including the sheet's today (today and the 89 days before it); a claim settled with no figure adds nothing. */
export function recentSettledCents(claims: Claim[], person: Person, today: string | undefined): bigint {
  if (today === undefined) return 0n;
  const since = plusDays(today, -89);
  return claims
    .filter((c) => c.person === person.id && c.status === 'SETTLED' && c.date >= since && c.date <= today)
    .reduce((sum, c) => sum + (c.settledDue === null ? 0n : hundredths(c.settledDue)), 0n);
}

/** Whether a person is active: they have an OPEN or PENDING claim that counts (see {@link counted}) dated within the
 *  30 days up to and including the sheet's today (today and the 29 days before it). */
export function isActive(claims: Claim[], person: Person, today: string | undefined): boolean {
  if (today === undefined) return false;
  const since = plusDays(today, -29);
  return counted(claims.filter((c) => c.person === person.id), [person]).some(
    (c) => isOpen(c) && c.date >= since && c.date <= today,
  );
}

/** Whether a claim is overdue: OPEN or PENDING, not deferred, with its due-by plus its grace before the sheet's today. */
export function overdue(claim: Claim, late: Late): boolean {
  return isOpen(claim) && !claim.deferred && late.today !== undefined && late.graceBy(claim) < late.today;
}

/** The overdue claims that count (see {@link counted}) among a person's or the sheet's claims, and the sum of
 *  their dues (in hundredths; a claim with no due adds nothing). */
export function overdueCents(
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  late: Late,
  floor: DatedCents,
): { count: number; cents: bigint } {
  const byId = new Map(people.map((p) => [p.id, p]));
  const overdueClaims = counted(claims, people).filter((c) => byId.has(c.person) && overdue(c, late));
  const cents = overdueClaims.reduce(
    (sum, c) => sum + (claimDueCents(c, claimRateOf(rates, gradeOn(byId.get(c.person)!, c.date), c), floor, late) ?? 0n),
    0n,
  );
  return { count: overdueClaims.length, cents };
}

/** A person's dues by the age of the claim on the sheet's today (the whole days from its date): their OPEN and
 *  PENDING claims that count (see {@link counted}) aged 0 to 30 days, 31 to 60, and 61 or more (in hundredths; a
 *  claim dated after today is 0 to 30, and a claim with no due adds nothing). */
export function agedCents(
  claims: Claim[],
  person: Person,
  rates: Rate[],
  late: Late,
  floor: DatedCents,
): { upTo30: bigint; upTo60: bigint; over60: bigint } {
  const aged = { upTo30: 0n, upTo60: 0n, over60: 0n };
  const today = late.today;
  for (const c of counted(claims.filter((c) => c.person === person.id), [person])) {
    if (!isOpen(c)) continue;
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    const days =
      today === undefined ? 0 : Math.round((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${c.date}T00:00:00Z`)) / 86_400_000);
    if (days <= 30) aged.upTo30 += due;
    else if (days <= 60) aged.upTo60 += due;
    else aged.over60 += due;
  }
  return aged;
}

/** Adds up the people's figures, leaving out the single highest and the single lowest when three or more people
 *  have a figure. */
export function trimmedSum(figures: bigint[]): bigint {
  const sum = figures.reduce((s, f) => s + f, 0n);
  if (figures.length < 3) return sum;
  const max = figures.reduce((m, f) => (f > m ? f : m));
  const min = figures.reduce((m, f) => (f < m ? f : m));
  return sum - max - min;
}

/** The sheet's totals: its claimed-total (everyone's counted claims added up,
 *  corrections left out), its due-total (everyone's due-total
 *  added up, skipping the people whose due-total is n/a, trimmed — see trimmedSum), and how many of the counted claims
 *  are overdue, with their total (each person's overdue-total, trimmed), and its settled-total and open-total. */
export function useSheetTotals(sheet: string): {
  claimed: string;
  due: bigint;
  overdue: { count: number; cents: bigint };
  settled: bigint;
  open: bigint;
} {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const cap = useCapCents(sheet);
  const due = trimmedSum(
    people
      .map((p) => personDueCents(all, rates, p, today, floor, late, cap))
      .filter((cents): cents is bigint => cents !== undefined),
  );
  const overdue = overdueCents(all, people, rates, late, floor);
  const perPerson = people.map((p) => overdueCents(all, [p], rates, late, floor).cents);
  // The settled-total and open-total: everyone's added up, not trimmed.
  const split = people.map((p) => splitDueCents(all, rates, p, floor, late));
  return {
    claimed: claimed(counted(all, people)),
    due,
    overdue: { count: overdue.count, cents: trimmedSum(perPerson) },
    settled: split.reduce((s, x) => s + x.settled, 0n),
    open: split.reduce((s, x) => s + x.open, 0n),
  };
}

/** Every tag ever carried by a claim on the sheet (`gone`: by claims since gone from it), or carried by a person on it,
 *  sorted. */
export function sheetTags(claims: Claim[], people: Person[] = [], gone: string[] = []): string[] {
  return [
    ...new Set([...claims.flatMap((c) => [...c.tagsEver, ...c.tags]), ...people.flatMap((p) => p.tags), ...gone]),
  ].sort();
}

/** The due-total of each tag ever used on the sheet (in hundredths): the dues of the claims that count (see
 *  {@link counted}) which carry it or whose person carries it, once each; a claim with two tags counting in both;
 *  a claim with no due adds nothing. */
export function useTagDueTotals(sheet: string): { tag: string; cents: bigint }[] {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const { data: shown } = useSheet(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map(sheetTags(all, people, shown?.tagsGone).map((t) => [t, 0n]));
  for (const c of counted(all, people)) {
    const person = byId.get(c.person);
    if (person === undefined) continue;
    const tags = new Set([...c.tags, ...person.tags]);
    if (tags.size === 0) continue;
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    for (const t of tags) totals.set(t, (totals.get(t) ?? 0n) + due);
  }
  return [...totals].map(([tag, cents]) => ({ tag, cents }));
}

/** The due-total of each calendar month (yyyy-mm) any claim on the sheet is dated in, sorted (in hundredths): the dues
 *  of the claims that count (see {@link counted}) dated in it; a claim with no due adds nothing. */
export function useMonthDueTotals(sheet: string): { month: string; cents: bigint }[] {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  const totals = new Map([...new Set(all.map((c) => c.date.slice(0, 7)))].sort().map((m) => [m, 0n]));
  for (const c of counted(all, people)) {
    const person = byId.get(c.person);
    if (person === undefined) continue;
    const due = claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late) ?? 0n;
    const month = c.date.slice(0, 7);
    totals.set(month, (totals.get(month) ?? 0n) + due);
  }
  return [...totals].map(([month, cents]) => ({ month, cents }));
}

/** Each person's due-total as their row shows it (see {@link personDueCents}): undefined when n/a. */
export function usePersonDueCentsOf(sheet: string): (person: Person) => bigint | undefined {
  const { data: all = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const cap = useCapCents(sheet);
  return (person) => personDueCents(all, rates, person, today, floor, late, cap);
}
