import { useQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A claim's status: DRAFT to start, then OPEN and SETTLED; a DRAFT or OPEN claim can be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim as the store holds it: the amount with two decimals, the date (YYYY-MM-DD), the note, the status and
 *  the number given when it was opened (the sheet's next, or the next in its period: P1-1, X-1; null for a claim never opened; a voided claim keeps it) and the due fixed when it was
 *  settled (null when it has none: its due is live), and who opened it (null when never opened); whether its person's last statement binds it, and the
 *  figure it binds it to (null: n/a); the number of the settled claim of the same person it corrects (null when it is not
 *  a correction — a correction is a claim with a negative amount); the date its rounding goes by (a correction's is the
 *  date of the claim it corrects, else its own). */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  due: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  roundsOn: string;
};

/** Whether a claim counts toward claimed and the totals: only OPEN and SETTLED claims do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

/** The ids of the people on hold. */
export const heldIds = (people: { id: number; held: boolean }[] | undefined) =>
  new Set((people ?? []).filter((p) => p.held).map((p) => p.id));

/** Whether a claim counts toward the live totals: as `counts`, except a held person's OPEN claims. */
export const countsLive = (c: Claim, held: Set<number>) => counts(c) && !(c.status === 'OPEN' && held.has(c.personId));

export const claimsQueryKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

export const claimsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/claims`;

export const personClaimsPath = (sheetId: string, personId: number) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: claimsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** An amount ("12.50") in whole cents, so sums are exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** A sum of the counted (OPEN and SETTLED) claims' amounts, with two decimals; a held person's OPEN claims
 *  are left out. */
export function sumOf(claims: Claim[], held: Set<number> = new Set()): string {
  return (claims.filter((c) => countsLive(c, held)).reduce((n, c) => n + cents(c.amount), 0) / 100).toFixed(2);
}

/** A claim's due-by (YYYY-MM-DD): its date plus the terms in force, in days. */
export function dueByOf(c: Claim, terms: string): string {
  const d = new Date(`${c.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + Number(terms));
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's today. */
export const isOverdue = (c: Claim, terms: string, today: string | undefined) =>
  c.status === 'OPEN' && today !== undefined && dueByOf(c, terms) < today;

/** The id of the claim whose editor is open (search param `editClaim`), or '' for none. */
export const useEditingClaim = () => useSearchParam('editClaim', asString);

/** The ids of the selected claims (search param `selectedClaims`, comma-separated), a toggle for one, and a
 *  clear. The toggle works from the URL as it is when it runs, so quick clicks in a row each keep the others'. */
export function useSelectedClaims() {
  const [ids] = useSearchParam('selectedClaims', asIds);
  const navigate = useNavigate();
  const update = (change: (ids: Set<number>) => void) =>
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        const set = new Set(asIds(prev.selectedClaims));
        change(set);
        if (set.size === 0) delete next.selectedClaims;
        else next.selectedClaims = [...set].join(',');
        return next;
      },
      replace: true,
    });
  return {
    selected: new Set(ids),
    toggle: (id: number, on: boolean) => update((set) => (on ? set.add(id) : set.delete(id))),
    clear: () => update((set) => set.clear()),
  };
}

const asIds = (raw: unknown): number[] =>
  raw === undefined || raw === null || raw === ''
    ? []
    : String(raw).split(',').map(Number).filter((n) => Number.isInteger(n));
