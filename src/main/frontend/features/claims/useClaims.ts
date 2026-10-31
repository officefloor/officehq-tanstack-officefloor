import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueCents, hundredths, roundTenThousandths } from '../rates/useRates';
import type { FloorOn } from '../settings/useSettings';

/** Where a claim is in its life: DRAFT → OPEN → SETTLED (or, settled by a clerk over the clerk
 *  limit, OPEN → PENDING, which a steward approves to SETTLED or rejects back to OPEN), and DRAFT,
 *  OPEN or PENDING → VOID. */
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
  /** The date (yyyy-mm-dd) the claim is deferred until; null while it is not deferred. */
  deferredUntil: string | null;
};

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** Every claim on the sheet, newest first, re-read whenever the sheet is loaded. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    staleTime: 0,
    gcTime: 0,
  });
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

/** What is due on one claim, in cents: the due fixed when it was settled, or else the figure its
 *  person's last statement bound it to, or else live at `pct`, at least the sheet's `floor` on the claim's date (null,
 *  n/a, when there is no rate). */
export function claimDueCents(claim: Claim, floor: FloorOn, pct: string | undefined, late?: Late): bigint | null {
  if (claim.due !== null) return hundredths(claim.due);
  const figure = claimFigureCents(claim, floor, pct);
  if (figure === null || late === undefined) return figure;
  return figure + lateChargeCents(claim, figure, late);
}

/** A claim's figure without any late charge, in cents: the figure its person's last statement bound
 *  it to, or else live at `pct`, at least the sheet's `floor` on the claim's date (null, n/a, when there is no rate).
 *  A PENDING claim is not bound by a statement: it shows its live figure. */
function claimFigureCents(claim: Claim, floor: FloorOn, pct: string | undefined): bigint | null {
  if (claim.stated && claim.status !== 'PENDING') return claim.statedDue !== null ? hundredths(claim.statedDue) : null;
  const due = dueCents(claim.amount, pct, claim.halfUp);
  if (due === null) return null;
  const least = hundredths(floor(claim.date));
  return due < least && !isCorrection(claim) ? least : due;
}

/** What makes an overdue claim's due grow: the sheet's `today` and its late percent in force on a
 *  claim's date, `pctOn`. */
export type Late = { today: string; pctOn: (date: string) => string };

/** Whole days from one yyyy-mm-dd to another. */
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

/** The late charge on an overdue claim's `figure`, in cents: the late percent of it for every full
 *  30 days it is overdue, rounded by the claim's rounding (`halfUp`). Nothing while it is not overdue. Never bound by a
 *  statement: it follows the sheet's today. */
export function lateChargeCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (!overdue(claim, late.today) || isCorrection(claim)) return 0n;
  const periods = BigInt(Math.floor(daysBetween(claim.dueBy, late.today) / 30));
  if (periods <= 0n) return 0n;
  // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
  const product = figure * hundredths(late.pctOn(claim.date)) * periods;
  return roundTenThousandths(product, claim.halfUp);
}


/** A person's due-total, in cents: the sum of their counted claims' figures, each already rounded —
 *  a settled claim's fixed due, else the figure a statement bound it to, else live (at least `floor`) at `pctOn(claim's date)`, the rate in force on the claim's
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

/** Whether a claim is overdue: it is OPEN (or PENDING), not deferred, and its due-by is before the sheet's `today`. */
export function overdue(claim: Claim, today: string): boolean {
  return isOpen(claim) && !claim.deferred && today !== '' && claim.dueBy < today;
}

/** These claims' overdue ones: their count, and their dues with late charges added up in cents (n/a ones skipped).
 *  `pctOn` is the rate in force on a claim's date. */
export function overdueFigures(claims: Claim[], late: Late, floor: FloorOn, pctOn: (date: string) => string | undefined): { count: number; cents: bigint } {
  const over = claims.filter((c) => overdue(c, late.today));
  const cents = over.reduce<bigint>((sum, c) => sum + (claimDueCents(c, floor, pctOn(c.date), late) ?? 0n), 0n);
  return { count: over.length, cents };
}
