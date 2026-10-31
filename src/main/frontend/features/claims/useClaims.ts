import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueCents, hundredths, roundTenThousandths } from '../rates/useRates';

/** Where a claim is in its life: DRAFT → OPEN → SETTLED, and DRAFT or OPEN → VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim as the server sends it: `amount` with two decimals, `date` as yyyy-mm-dd. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  /** Given when the claim is opened (a voided claim keeps it); null for a DRAFT, or a claim voided while a DRAFT. */
  number: number | null;
  /** The due fixed when the claim was settled, two decimals; null when it has none, so its due is live. */
  due: string | null;
  /** The user who opened the claim; null while it has not been opened. */
  openedBy: string | null;
  /** Whether the person's last statement binds the claim to the figure it had then. */
  stated: boolean;
  /** The figure the last statement bound the claim to, two decimals; null when unbound or bound to n/a. */
  statedDue: string | null;
  /** The number of the SETTLED claim of the same person a correction (a negative amount) corrects; null for any other claim. */
  corrects: number | null;
  /** Whether the claim's live figures (its rate step and late charge) round half up, else half to
   *  even: half up when it is dated before the sheet's policy date (a correction goes by the date of
   *  the claim it corrects). */
  halfUp: boolean;
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
export const CLAIM_NUMBER = /^\d+$/;

/** Whether a claim is a correction: its amount is negative. Its due has no floor and no late charge. */
export function isCorrection(claim: Claim): boolean {
  return claim.amount.startsWith('-');
}

/** The claims that count toward claimed, due-total and every total: OPEN and SETTLED ones. */
export function counted(claims: Claim[]): Claim[] {
  return claims.filter((c) => c.status === 'OPEN' || c.status === 'SETTLED');
}

/** The claims that count in the live totals: OPEN and SETTLED ones, but not the OPEN ones of a
 *  held person (`held` tells, by person id). */
export function live(claims: Claim[], held: (personId: number) => boolean): Claim[] {
  return counted(claims).filter((c) => c.status !== 'OPEN' || !held(c.personId));
}

/** The sum of these claims' amounts, with two decimals. Added up in cents, so it is exact. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** What is due on one claim, in cents: the due fixed when it was settled, or else the figure its
 *  person's last statement bound it to, or else live at `pct`, at least the sheet's `floor` (null,
 *  n/a, when there is no rate). */
export function claimDueCents(claim: Claim, floor: string, pct: string | undefined, late?: Late): bigint | null {
  if (claim.due !== null) return hundredths(claim.due);
  const figure = claimFigureCents(claim, floor, pct);
  if (figure === null || late === undefined) return figure;
  return figure + lateChargeCents(claim, figure, late);
}

/** A claim's figure without any late charge, in cents: the figure its person's last statement bound
 *  it to, or else live at `pct`, at least the sheet's `floor` (null, n/a, when there is no rate). */
function claimFigureCents(claim: Claim, floor: string, pct: string | undefined): bigint | null {
  if (claim.stated) return claim.statedDue !== null ? hundredths(claim.statedDue) : null;
  const due = dueCents(claim.amount, pct, claim.halfUp);
  if (due === null) return null;
  const least = hundredths(floor);
  return due < least && !isCorrection(claim) ? least : due;
}

/** What makes an overdue claim's due grow: the sheet's `terms`, its `today` and its late percent `pct`. */
export type Late = { terms: number; today: string; pct: string };

/** Whole days from one yyyy-mm-dd to another. */
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

/** The late charge on an overdue claim's `figure`, in cents: the late percent of it for every full
 *  30 days it is overdue, rounded by the claim's rounding (`halfUp`). Nothing while it is not overdue. Never bound by a
 *  statement: it follows the sheet's today. */
export function lateChargeCents(claim: Claim, figure: bigint, late: Late): bigint {
  if (!overdue(claim, late.terms, late.today) || isCorrection(claim)) return 0n;
  const periods = BigInt(Math.floor(daysBetween(dueBy(claim, late.terms), late.today) / 30));
  if (periods <= 0n) return 0n;
  // cents * hundredths-of-a-percent is in ten-thousandths of a cent.
  const product = figure * hundredths(late.pct) * periods;
  return roundTenThousandths(product, claim.halfUp);
}


/** A person's due-total, in cents: the sum of their counted claims' figures, each already rounded —
 *  a settled claim's fixed due, else the figure a statement bound it to, else live (at least `floor`) at `pctOn(claim's date)`, the rate in force on the claim's
 *  date (at `pct`, the rate in force on the sheet's today, when no `pctOn` is given). 0 with
 *  nothing that counts. With `late`, an overdue claim's late charge is added on top. Null (n/a) when there is no rate today, unless some settled claim keeps a
 *  figure: then it is the sum of the figures there are. */
export function personDueCents(claims: Claim[], floor: string, pct: string | undefined, pctOn?: (date: string) => string | undefined, late?: Late): bigint | null {
  let sum = 0n;
  let any = false;
  for (const c of counted(claims)) {
    const due = claimDueCents(c, floor, pctOn ? pctOn(c.date) : pct, late);
    if (due === null) continue;
    sum += due;
    any = true;
  }
  return pct === undefined && !any ? null : sum;
}

/** A claim's due-by (yyyy-mm-dd): its date plus the sheet's `terms` in days. */
export function dueBy(claim: Claim, terms: number): string {
  const d = new Date(`${claim.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's `today`. */
export function overdue(claim: Claim, terms: number, today: string): boolean {
  return claim.status === 'OPEN' && today !== '' && dueBy(claim, terms) < today;
}

/** These claims' overdue ones: their count, and their dues with late charges added up in cents (n/a ones skipped).
 *  `pctOn` is the rate in force on a claim's date. */
export function overdueFigures(claims: Claim[], late: Late, floor: string, pctOn: (date: string) => string | undefined): { count: number; cents: bigint } {
  const over = claims.filter((c) => overdue(c, late.terms, late.today));
  const cents = over.reduce<bigint>((sum, c) => sum + (claimDueCents(c, floor, pctOn(c.date), late) ?? 0n), 0n);
  return { count: over.length, cents };
}
