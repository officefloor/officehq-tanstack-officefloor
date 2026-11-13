import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED; DRAFT or OPEN -> VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/**
 * A claim as the store holds it: the amount with two decimals, the date as yyyy-MM-dd; its number
 * is the sheet's next one when it was opened, null while a draft (a voided claim keeps it); its due is the
 * one fixed when it was settled (two decimals), null while it is live; openedBy is the user who
 * opened it, null while a draft; stated is whether its person's last statement bound it to a figure,
 * statedDue that figure (two decimals; null: n/a) — it then shows and totals that, not its live figure.
 */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  due: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
};

/**
 * Whether a claim counts toward claimed, due-total and every live total: only OPEN and SETTLED do, and
 * an OPEN one not while its person is held.
 */
export const counts = (c: Claim, held = false) => c.status === 'SETTLED' || (c.status === 'OPEN' && !held);

/** The ids of the people on hold, whose open claims count in no live total. */
export const heldIds = (people: { id: number; held: boolean }[]) => new Set(people.filter((p) => p.held).map((p) => p.id));

/** The fields the claim form submits; a blank date is the sheet's today. */
export type ClaimInput = { amount: string; date: string; note: string };

/** 0.00 or more, with at most two decimals. */
export const AMOUNT = /^(\d+(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's claims; under sheetKey, so re-loading the sheet re-reads them. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: !!sheet,
  });
}

/** The sum of the counted claims' amounts, with two decimals; added in cents so it is exact. `held` holds the people on hold. */
export function claimed(claims: Claim[], held: Set<number>): string {
  const cents = claims.filter((c) => counts(c, held.has(c.personId))).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
