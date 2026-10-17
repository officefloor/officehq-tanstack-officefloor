import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

// Amounts travel as strings with two decimals ("13.50"); sums are added up in whole cents so no
// floating-point error shows.
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };
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

export const updateClaim = (sheet: string, user: string, id: number, input: ClaimInput) =>
  putJson<Claim>(`${base(sheet)}/claims/${id}`, input, user);

const cents = (amount: string) => {
  const [whole, frac = ''] = amount.split('.');
  return Number(whole) * 100 + Number((frac + '00').slice(0, 2));
};

/** The amounts added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  const sum = claims.reduce((n, c) => n + cents(c.amount), 0);
  return `${Math.floor(sum / 100)}.${String(sum % 100).padStart(2, '0')}`;
}
