import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { gradeOn, heldOn, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { dueCents, hundredths, rateOf, twoDecimals, useRates, type Rate } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useCapCents, useFloorCents, useLatePctCents, usePolicyDate, type DatedCents } from '../settings/settings';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN/PENDING -> VOID. A clerk's settle of a
 *  due above the clerk limit makes it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN). */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** Every status, in the order the screen lists them. */
export const CLAIM_STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** How many of the claims have the status (every claim, whoever's). */
export function statusCount(claims: Claim[], status: ClaimStatus): number {
  return claims.filter((c) => c.status === status).length;
}

/** A status change a claim's row offers; the server refuses one its status does not allow. */
export type ClaimStatusChange = 'open' | 'settle' | 'void' | 'approve' | 'reject';

/** Whether a claim is still open: OPEN, or PENDING (handled like OPEN, but not bound by a statement). */
export function isOpen(claim: Claim): boolean {
  return claim.status === 'OPEN' || claim.status === 'PENDING';
}

/** Whether a claim's statement binds its figure: it is stated and not PENDING (which shows its live figure). */
export function bound(claim: Claim): boolean {
  return claim.stated && claim.status !== 'PENDING';
}

/** A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its number on
 * the sheet (given when it is opened; null while a draft; kept once voided), and its due fixed when it was
 * settled (two decimals; null while live: until settled, and when its grade had no rate then), and the
 * user who opened it (null until opened; that user may not settle it), and whether its person's last statement
 * bound it, with the figure it bound (null when its grade had no rate then), and the number the claim it
 * corrects shows (null unless it is a correction: a negative amount), and the date it rounds on (its own, or for a
 * correction the date of the claim it corrects), and its number within its period (<period>-<n>, or X-<n> in no
 * period) when it was opened while the sheet numbered by period, shown in place of its number (null otherwise), and
 * its due-by (yyyy-MM-dd), fixed when it was added from the terms of its person's grade in force on its date, and
 * its status log, oldest first, and the date it was last deferred until (null when never), with whether it is
 * deferred now (OPEN with that date after the sheet's today: it counts in no total, is not overdue and cannot be
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
};

/** One line of a claim's status log: the status it took, by whom, on the sheet's today then (yyyy-MM-dd), and the
 * claim's due as computed at that moment (two decimals, or n/a). */
export type ClaimLogLine = { status: ClaimStatus; by: string; on: string; due: string };

export const claimsKey = (sheet: string) => ['claims', sheet] as const;

export const claimsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet)),
  });
}

/** The claims that count toward claimed, due-total and every total: the OPEN, PENDING and SETTLED ones, but not
 *  the deferred ones, nor the OPEN or PENDING ones dated within a hold of their person, unless bound by a statement. */
export function counted(claims: Claim[], people: Person[]): Claim[] {
  const byId = new Map(people.map((p) => [p.id, p]));
  return claims.filter((c) => {
    if (c.deferred) return false;
    if (c.status === 'SETTLED') return true;
    if (!isOpen(c)) return false;
    const person = byId.get(c.person);
    return bound(c) || person === undefined || !heldOn(person, c.date);
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
 *  dated within a hold of its person. */
export function countedOn(claim: Claim, person: Person | undefined, end: string): boolean {
  const then = statusOn(claim, end);
  if (claim.deferred || then === undefined) return false;
  if (then === 'SETTLED') return true;
  if (then !== 'OPEN' && then !== 'PENDING') return false;
  return !isOpen(claim) || bound(claim) || person === undefined || !heldOn(person, claim.date);
}

/** The sum of the claims' amounts with two decimals, added in whole cents so no float error shows. */
export function claimed(claims: Claim[]): string {
  return twoDecimals(claims.reduce((sum, c) => sum + hundredths(c.amount), 0n));
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
 *  (yyyy-MM-dd; undefined: its today), which decides how a claim's live figures round. */
export type Late = { pct: DatedCents; today: string | undefined; policy: string | undefined };

/** The sheet's late percent, today and policy date. */
export function useLate(sheet: string): Late {
  const today = useSheetToday(sheet);
  const policy = usePolicyDate(sheet);
  return { pct: useLatePctCents(sheet), today, policy: policy === '' ? today : policy };
}

/** Whether a claim's live figures round half up: it rounds on a date (its own, or for a correction the date of the
 *  claim it corrects) before the policy date; on or after it they round half to even. */
export function roundsHalfUp(claim: Claim, late: Late): boolean {
  return late.policy !== undefined && claim.roundsOn < late.policy;
}

/** A claim's figure: the one fixed when it was settled, else the one its statement bound, else its amount at the
 *  grade's rate in force on its date, raised to at least the sheet's floor in force on its date (in hundredths; a correction has no floor). */
export function claimFigureCents(claim: Claim, pct: string | undefined, floor: DatedCents, halfUp = false): bigint | undefined {
  if (claim.settledDue !== null) return hundredths(claim.settledDue);
  if (bound(claim)) return claim.statedDue === null ? undefined : hundredths(claim.statedDue);
  const live = dueCents(claim.amount, pct, halfUp);
  const least = floor(claim.date);
  return live === undefined || correction(claim) || live >= least ? live : least;
}

/** The whole days a claim is overdue: from its due-by to the sheet's today (0 when it is not overdue). */
function daysOverdue(claim: Claim, late: Late): number {
  if (!overdue(claim, late.today)) return 0;
  return Math.round((Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${claim.dueBy}T00:00:00Z`)) / 86_400_000);
}

/** The day (yyyy-MM-dd) the given number of days after a day. */
function plusDays(day: string, days: number): string {
  return new Date(Date.parse(`${day}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/** A claim's late charge on its figure (in hundredths): for each full 30 days it is overdue, the late percent in
 *  force on that boundary (its due-by plus 30, 60, 90 ... days) of the figure, rounded as the claim's live figures
 *  are (see {@link roundsHalfUp}), the charges added up; a correction has none. */
export function lateCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (correction(claim)) return 0n;
  const periods = Math.floor(daysOverdue(claim, late) / 30);
  let total = 0n;
  for (let k = 1; k <= periods; k++) {
    const pct = late.pct(plusDays(claim.dueBy, 30 * k));
    if (pct === 0n) continue;
    const product = figure * pct;
    const quotient = product / 10000n;
    const remainder = product % 10000n;
    const up = remainder > 5000n || (remainder === 5000n && (roundsHalfUp(claim, late) || quotient % 2n === 1n));
    total += up ? quotient + 1n : quotient;
  }
  return total;
}

/** A claim's due: its figure plus, while it is overdue, its late charge (settling fixes the charge in). */
export function claimDueCents(claim: Claim, pct: string | undefined, floor: DatedCents, late: Late): bigint | undefined {
  const figure = claimFigureCents(claim, pct, floor, roundsHalfUp(claim, late));
  if (figure === undefined || claim.settledDue !== null) return figure;
  return figure + lateCents(claim, figure, late);
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
    return fixed(c) || (then !== undefined && then !== person.grade && claimFigureCents(c, rateOf(rates, then, c.date), floor) !== undefined)
      || (then !== undefined && today !== undefined && c.date <= today && claimFigureCents(c, rateOf(rates, then, c.date), floor) !== undefined);
  };
  if (rateOf(rates, person.grade, today) === undefined && !mine.some(apart)) return undefined;
  return mine.reduce((sum, c) => sum + (claimDueCents(c, rateOf(rates, gradeOn(person, c.date), c.date), floor, late) ?? 0n), 0n);
}

/** Whether a claim is overdue: OPEN or PENDING, not deferred, with its due-by before the sheet's today. */
export function overdue(claim: Claim, today: string | undefined): boolean {
  return isOpen(claim) && !claim.deferred && today !== undefined && claim.dueBy < today;
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
  const overdueClaims = counted(claims, people).filter((c) => byId.has(c.person) && overdue(c, late.today));
  const cents = overdueClaims.reduce(
    (sum, c) => sum + (claimDueCents(c, rateOf(rates, gradeOn(byId.get(c.person)!, c.date), c.date), floor, late) ?? 0n),
    0n,
  );
  return { count: overdueClaims.length, cents };
}

/** The sheet's totals: its claimed-total (everyone's counted claims added up), its due-total (everyone's due-total
 *  added up, skipping the people whose due-total is n/a), and how many of the counted claims are overdue, with their
 *  dues (late charges included) added up. */
export function useSheetTotals(sheet: string): { claimed: string; due: bigint; overdue: { count: number; cents: bigint } } {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const cap = useCapCents(sheet);
  const due = people.reduce((sum, p) => {
    const cents = personDueCents(all, rates, p, today, floor, late, cap);
    return cents === undefined ? sum : sum + cents;
  }, 0n);
  return { claimed: claimed(counted(all, people)), due, overdue: overdueCents(all, people, rates, late, floor) };
}
