import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { dueCents, hundredths, rateOf, twoDecimals, type Rate } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useLatePctCents, usePolicyDate } from '../settings/settings';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN/PENDING -> VOID. A clerk's settle of a
 *  due above the clerk limit makes it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN). */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

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
 * its status log, oldest first. */
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
 *  the OPEN or PENDING ones of a person on hold. */
export function counted(claims: Claim[], people: Person[]): Claim[] {
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => c.status === 'SETTLED' || (isOpen(c) && !held.has(c.person)));
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

/** What makes an overdue claim's late charge: the sheet's late percent (in hundredths) and its today.
 *  The charge is never bound by a statement: it follows the sheet's today. It also carries the sheet's policy date
 *  (yyyy-MM-dd; undefined: its today), which decides how a claim's live figures round. */
export type Late = { pct: bigint; today: string | undefined; policy: string | undefined };

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
 *  grade's rate in force on its date, raised to at least the sheet's floor (in hundredths; a correction has no floor). */
export function claimFigureCents(claim: Claim, pct: string | undefined, floor: bigint, halfUp = false): bigint | undefined {
  if (claim.settledDue !== null) return hundredths(claim.settledDue);
  if (bound(claim)) return claim.statedDue === null ? undefined : hundredths(claim.statedDue);
  const live = dueCents(claim.amount, pct, halfUp);
  return live === undefined || correction(claim) || live >= floor ? live : floor;
}

/** The whole days a claim is overdue: from its due-by to the sheet's today (0 when it is not overdue). */
function daysOverdue(claim: Claim, late: Late): number {
  if (!overdue(claim, late.today)) return 0;
  return Math.round((Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${claim.dueBy}T00:00:00Z`)) / 86_400_000);
}

/** A claim's late charge on its figure (in hundredths): the late percent of the figure for every full 30 days it is
 *  overdue, rounded as the claim's live figures are (see {@link roundsHalfUp}); a correction has none. */
export function lateCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (correction(claim)) return 0n;
  const periods = BigInt(Math.floor(daysOverdue(claim, late) / 30));
  const product = figure * late.pct * periods;
  const quotient = product / 10000n;
  const remainder = product % 10000n;
  if (remainder > 5000n || (remainder === 5000n && (roundsHalfUp(claim, late) || quotient % 2n === 1n))) return quotient + 1n;
  return quotient;
}

/** A claim's due: its figure plus, while it is overdue, its late charge (settling fixes the charge in). */
export function claimDueCents(claim: Claim, pct: string | undefined, floor: bigint, late: Late): bigint | undefined {
  const figure = claimFigureCents(claim, pct, floor, roundsHalfUp(claim, late));
  if (figure === undefined || claim.settledDue !== null) return figure;
  return figure + lateCents(claim, figure, late);
}


/** A person's due-total: the sum of their counted claims' rounded figures (the claims of others are skipped). n/a (undefined) when their grade
 *  has no rate on the sheet's today, unless a claim has a figure apart from it (settled, stated, or at another grade
 *  in force on its date): then the figures there are. */
export function personDueCents(
  claims: Claim[],
  rates: Rate[],
  person: Person,
  today: string | undefined,
  floor: bigint,
  late: Late,
): bigint | undefined {
  const mine = counted(claims.filter((c) => c.person === person.id), [person]);
  // A claim has a figure apart from today's grade when it keeps one, or when another grade in force on its date gives one.
  const apart = (c: Claim) => {
    const then = gradeOn(person, c.date);
    return fixed(c) || (then !== undefined && then !== person.grade && claimFigureCents(c, rateOf(rates, then, c.date), floor) !== undefined);
  };
  if (rateOf(rates, person.grade, today) === undefined && !mine.some(apart)) return undefined;
  return mine.reduce((sum, c) => sum + (claimDueCents(c, rateOf(rates, gradeOn(person, c.date), c.date), floor, late) ?? 0n), 0n);
}

/** Whether a claim is overdue: OPEN or PENDING with its due-by before the sheet's today. */
export function overdue(claim: Claim, today: string | undefined): boolean {
  return isOpen(claim) && today !== undefined && claim.dueBy < today;
}

/** The overdue claims that count (see {@link counted}) among a person's or the sheet's claims, and the sum of
 *  their dues (in hundredths; a claim with no due adds nothing). */
export function overdueCents(
  claims: Claim[],
  people: Person[],
  rates: Rate[],
  late: Late,
  floor: bigint,
): { count: number; cents: bigint } {
  const byId = new Map(people.map((p) => [p.id, p]));
  const overdueClaims = counted(claims, people).filter((c) => byId.has(c.person) && overdue(c, late.today));
  const cents = overdueClaims.reduce(
    (sum, c) => sum + (claimDueCents(c, rateOf(rates, gradeOn(byId.get(c.person)!, c.date), c.date), floor, late) ?? 0n),
    0n,
  );
  return { count: overdueClaims.length, cents };
}
