import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueCents, hundredths, roundTenThousandths } from '../rates/useRates';
import type { FloorOn } from '../settings/useSettings';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** Where a claim is in its life: DRAFT → OPEN → SETTLED (or, settled by a clerk over the clerk
 *  limit, OPEN → PENDING, which a steward approves to SETTLED or rejects back to OPEN), and DRAFT,
 *  OPEN or PENDING → VOID; a steward may void a SETTLED claim within 7 days of its settlement. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** Every status, in life order — the order the summary and header list them in. */
export const STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** Whether a claim is still open: OPEN or PENDING. A PENDING claim is handled like an OPEN one: it
 *  goes overdue, bears the late charge, and a hold takes it out of the totals. */
export function isOpen(claim: Claim): boolean {
  return claim.status === 'OPEN' || claim.status === 'PENDING';
}

/** A claim as the server sends it: `amount` with two decimals, `date` as yyyy-mm-dd. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  /** Given when the claim is opened (a voided claim keeps it); null for a DRAFT, or a claim voided while a DRAFT. Sheet-wide ("7"), or within its period ("P1-2", "X-1" in no period). */
  number: string | null;
  /** The due fixed when the claim was settled, two decimals; null when it has none, so its due is live. */
  due: string | null;
  /** The user who opened the claim; null while it has not been opened. */
  openedBy: string | null;
  /** Whether the person's last statement binds the claim to the figure it had then. */
  stated: boolean;
  /** The figure the last statement bound the claim to, two decimals; null when unbound or bound to n/a. */
  statedDue: string | null;
  /** The number of the SETTLED claim of the same person a correction (a negative amount) corrects; null for any other claim. */
  corrects: string | null;
  /** Whether the claim's live figures (its rate step and late charge) round half up, else half to
   *  even: half up when it is dated before the sheet's policy date (a correction goes by the date of
   *  the claim it corrects). */
  halfUp: boolean;
  /** The date the claim is due by (yyyy-mm-dd), fixed when it was added from the terms of its
   *  person's grade then; later changes to the terms or the claim's date leave it. */
  dueBy: string;
  /** The claim's status log, oldest first: one line per status it has had, as
   *  "<STATUS> by <user> on <date> due=<due>" (the sheet's today and the claim's due then). */
  log: string[];
  /** Whether the claim is deferred now: OPEN, and the sheet's today is before its deferred-until
   *  date. A deferred claim counts in no total, is not overdue and cannot be settled. */
  deferred: boolean;
  /** The date (yyyy-mm-dd) the claim was last deferred until (kept once that date is reached); null when it never was. */
  deferredUntil: string | null;
  /** How many proofs the claim has: one whose due is above the sheet's proof threshold on its date
   *  cannot be settled or approved while it has none. */
  proofs: number;
  /** The number of the claim this one was split off; null when it was not split off one. */
  splitFrom: string | null;
  /** The user whose open editor locks the claim for everyone else; null when no editor holds it. */
  lockedBy: string | null;
  /** Whether the claim was added while its period was soft-closed. */
  lateEntry: boolean;
  /** The last day (yyyy-mm-dd) a reminder was written for the claim; null when it never was. */
  remindedOn: string | null;
  /** The claim's external reference, unique on the sheet without regard to case; null when it has none. */
  ref: string | null;
  /** The claim's tags, lower-cased and sorted; empty when it has none. */
  tags: string[];
  /** Every tag the claim has ever carried (its tags now among them). */
  tagsUsed: string[];
  /** The reason from the sheet's list the claim was voided with; null when none was given. */
  voidReason: string | null;
  /** The claim's comments, oldest first, as "<user> on <date>: <text>" (the sheet's today when it was written). */
  comments: string[];
  /** The number of the bulk settle batch (1, 2, ... on the sheet) that settled the claim (or sent it PENDING); null when none did. */
  batch: number | null;
};

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** Every claim on the sheet, newest first, re-read whenever the sheet is loaded. */
export function useClaims(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(claimsKey(sheet), asOf),
    queryFn: () => getJson<Claim[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/claims`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}

/** The number of the sheet's last bulk settle batch; null when there has been none. */
export function lastBatch(claims: Claim[]): number | null {
  return claims.reduce<number | null>((last, c) => (c.batch != null && (last == null || c.batch > last) ? c.batch : last), null);
}

/** An amount the server accepts: up to two decimals; negative only for a correction. */
export const AMOUNT = /^-?\d+(\.\d{1,2})?$/;

/** A correction's `corrects`: the number of the claim it corrects. */
export const CLAIM_NUMBER = /^(\d+|.+-\d+)$/;

/** Whether a claim is a correction: its amount is negative. Its due has no floor and no late charge. */
export function isCorrection(claim: Claim): boolean {
  return claim.amount.startsWith('-');
}

/** The claims that count toward claimed, due-total and every total: OPEN, PENDING and SETTLED ones. */
export function counted(claims: Claim[]): Claim[] {
  return claims.filter((c) => isOpen(c) || c.status === 'SETTLED');
}

/** The claims that count in the live totals: OPEN, PENDING and SETTLED ones, but not deferred ones,
 *  nor the OPEN or PENDING ones dated within a hold of their person's (`heldOn` tells, by person id and date),
 *  unless a statement binds them. */
export function live(claims: Claim[], heldOn: (personId: number, date: string) => boolean): Claim[] {
  return counted(claims).filter(
    (c) => !c.deferred && (!isOpen(c) || (c.stated && c.status !== 'PENDING') || !heldOn(c.personId, c.date)),
  );
}

/** The sum of these claims' amounts, with two decimals. Added up in cents, so it is exact. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** What is due on one claim, in cents: the due fixed when it was settled, or else the greater of the
 *  sheet's `floor` on the claim's date and its figure (the one its person's last statement bound it to,
 *  or else live at `pct`) plus its late charge (null, n/a, when there is no rate). A correction has
 *  neither floor nor charge. */
export function claimDueCents(claim: Claim, floor: FloorOn, pct: string | undefined, late?: Late): bigint | null {
  if (claim.due !== null) return hundredths(claim.due);
  const figure = claimFigureCents(claim, pct);
  if (figure === null || isCorrection(claim)) return figure;
  const due = late === undefined ? figure : figure + lateChargeCents(claim, figure, late);
  const least = hundredths(floor(claim.date));
  return due < least ? least : due;
}

/** A claim's figure without any late charge or floor, in cents: the figure its person's last statement
 *  bound it to, or else live at `pct` (null, n/a, when there is no rate).
 *  A PENDING claim is not bound by a statement: it shows its live figure. */
function claimFigureCents(claim: Claim, pct: string | undefined): bigint | null {
  if (claim.stated && claim.status !== 'PENDING') return claim.statedDue !== null ? hundredths(claim.statedDue) : null;
  return dueCents(claim.amount, pct, claim.halfUp);
}

/** What makes an overdue claim's due grow: the sheet's `today` and its late percent in force on a
 *  date, `pctOn`; and the grace days a claim has past its due-by, `graceOn` (none when not given). */
export type Late = { today: string; pctOn: (date: string) => string; graceOn?: (claim: Claim) => number };

/** Whole days from one yyyy-mm-dd to another. */
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

/** The yyyy-mm-dd `days` days after `date`. */
function addDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/** The date a claim goes overdue after: its due-by plus its grace days. */
export function overdueFrom(claim: Claim, late: Late): string {
  return addDays(claim.dueBy, late.graceOn?.(claim) ?? 0);
}

/** The late charge on an overdue claim's `figure`, in cents: for every full 30 days it is overdue,
 *  counted from its due-by plus its grace, the late percent in force on that boundary date (the due-by
 *  plus the grace plus 30, 60, 90 ... days) of it plus the charges of the boundaries before it (compounding), each
 *  rounded by the claim's rounding (`halfUp`), added up; a boundary at 0.00 adds nothing. Nothing
 *  while it is not overdue. Never bound by a statement: it follows the sheet's today. */
export function lateChargeCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (!overdue(claim, late) || isCorrection(claim)) return 0n;
  const from = overdueFrom(claim, late);
  const periods = Math.floor(daysBetween(from, late.today) / 30);
  let total = 0n;
  for (let k = 1; k <= periods; k++) {
    const pct = hundredths(late.pctOn(addDays(from, 30 * k)));
    if (pct === 0n) continue;
    // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
    total += roundTenThousandths((figure + total) * pct, claim.halfUp);
  }
  return total;
}


/** A person's due-total, in cents: the sum of their counted claims' figures, each already rounded —
 *  a settled claim's fixed due, else the figure a statement bound it to, else live at `pctOn(claim's date)`, plus any late charge and at least `floor`, the rate in force on the claim's
 *  date (at `pct`, the rate in force on the sheet's today, when no `pctOn` is given). 0 with
 *  nothing that counts. With `late`, an overdue claim's late charge is added on top. Null (n/a) when there is no rate today, unless some settled claim keeps a
 *  figure: then it is the sum of the figures there are. With `cap`, it is at most the cap (an n/a one
 *  is not capped). */
export function personDueCents(claims: Claim[], floor: FloorOn, pct: string | undefined, pctOn?: (date: string) => string | undefined, late?: Late, cap?: string | null): bigint | null {
  let sum = 0n;
  let any = false;
  for (const c of counted(claims)) {
    const due = claimDueCents(c, floor, pctOn ? pctOn(c.date) : pct, late);
    if (due === null) continue;
    sum += due;
    any = true;
  }
  if (pct === undefined && !any) return null;
  return capCents(sum, cap);
}

/** A due-total at most the sheet's `cap` (null or blank: no cap); n/a is not capped. */
export function capCents(due: bigint | null, cap?: string | null): bigint | null {
  if (due === null || cap == null || cap === '') return due;
  const most = hundredths(cap);
  return due > most ? most : due;
}

/** How much a due-total is over the sheet's `cap`: 0 when it is not (or is n/a, or there is no cap). */
export function excessCents(due: bigint | null, cap?: string | null): bigint {
  const capped = capCents(due, cap);
  return due === null || capped === null ? 0n : due - capped;
}

/** Whether a claim is overdue: it is OPEN (or PENDING), not deferred, and its due-by plus its grace
 *  days is before the sheet's today. */
export function overdue(claim: Claim, late: Late): boolean {
  return isOpen(claim) && !claim.deferred && late.today !== '' && overdueFrom(claim, late) < late.today;
}

/** These claims' overdue ones: their count, and their dues with late charges added up in cents (n/a ones skipped).
 *  `pctOn` is the rate in force on a claim's date. */
export function overdueFigures(claims: Claim[], late: Late, floor: FloorOn, pctOn: (date: string) => string | undefined): { count: number; cents: bigint } {
  const over = claims.filter((c) => overdue(c, late));
  const cents = over.reduce<bigint>((sum, c) => sum + (claimDueCents(c, floor, pctOn(c.date), late) ?? 0n), 0n);
  return { count: over.length, cents };
}
