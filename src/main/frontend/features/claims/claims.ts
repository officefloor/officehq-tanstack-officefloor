import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

// Amounts travel as strings with two decimals ("13.50"); sums are added up in whole cents so no
// floating-point error shows.
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when opened, the next on the sheet; null for a draft; a voided claim keeps it. */
  number: number | null;
  /** Fixed when settled (two decimals); null while live, or when no rate applied at settling. */
  settledDue: string | null;
  /** The user who opened it; null for a draft. */
  openedBy: string | null };
export type ClaimInput = { amount: string; date: string; note: string };

/** 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}`;
export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** The active sheet's claims, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsKey(sheet ?? ''),
    queryFn: () => getJson<{ claims: Claim[] }>(`${base(sheet!)}/claims`).then((r) => r.claims),
    enabled: !!sheet,
  });
}

export const addClaim = (sheet: string, user: string, personId: number, input: ClaimInput) =>
  postJson<Claim>(`${base(sheet)}/people/${personId}/claims`, input, user);

/** The quick-add bar: a DRAFT claim dated the sheet's today with an empty note, for the named person
 *  (created with grade A when not on the sheet). */
export const quickAddClaim = (sheet: string, user: string, input: { name: string; amount: string }) =>
  postJson<Claim>(`${base(sheet)}/quick-claims`, input, user);

export const updateClaim = (sheet: string, user: string, id: number, input: ClaimInput) =>
  putJson<Claim>(`${base(sheet)}/claims/${id}`, input, user);

// A claim starts DRAFT; it can be opened, then settled; a DRAFT or OPEN claim can be voided.
// SETTLED and VOID are final. The server refuses a change not allowed; the screen refuses it first.
export type ClaimAction = 'open' | 'settle' | 'void';
export const CLAIM_ACTIONS: ClaimAction[] = ['open', 'settle', 'void'];
const ALLOWED: Record<ClaimAction, ClaimStatus[]> = { open: ['DRAFT'], settle: ['OPEN'], void: ['DRAFT', 'OPEN'] };
export const canTake = (claim: Claim, action: ClaimAction) => ALLOWED[action].includes(claim.status);

/** Only a DRAFT can be edited; to change an OPEN claim, void it and add a new one. */
export const canEdit = (claim: Claim) => claim.status === 'DRAFT';

/** Four eyes: the user who opened a claim cannot settle it. */
export const selfSettle = (claim: Claim, user: string, action: ClaimAction) =>
  action === 'settle' && claim.openedBy != null && claim.openedBy === user.trim();

export const claimAction = (sheet: string, user: string, id: number, action: ClaimAction) =>
  postJson<Claim>(`${base(sheet)}/claims/${id}/${action}`, {}, user);

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
export const counts = (claim: Claim) => claim.status === 'OPEN' || claim.status === 'SETTLED';

const cents = (amount: string) => {
  const [whole, frac = ''] = amount.split('.');
  return Number(whole) * 100 + Number((frac + '00').slice(0, 2));
};

/** The amounts of the claims that count (OPEN and SETTLED) added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  const sum = claims.filter(counts).reduce((n, c) => n + cents(c.amount), 0);
  return `${Math.floor(sum / 100)}.${String(sum % 100).padStart(2, '0')}`;
}
