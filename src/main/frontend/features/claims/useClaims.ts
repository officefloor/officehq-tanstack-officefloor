import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's status: DRAFT -> OPEN -> SETTLED; a DRAFT or OPEN claim can become VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim: its amount with two decimals, its date (yyyy-mm-dd), its note, its status, its number on
 *  the sheet (given when it is opened: 1, 2, ... or, under period numbering, <period>-<n> or X-<n>; null while a draft and once void) and the due fixed when it was
 *  settled (null unless settled while its grade had a rate: its due is then live, from the rates), and
 *  who opened it (null until it is opened), whether its person's last statement bound it, and the figure
 *  it was bound to (null when not bound, or bound with no figure: its due then stays live), and, for a
 *  correction (a negative amount), the number of the settled claim of the same person it corrects. */
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
};

/** A claim's fixed figure: the due frozen when it was settled, else the one its last statement bound
 *  it to; null when its due is live. */
export function fixedDue(c: Claim): string | null {
  return c.due ?? (c.stated ? c.statedDue : null);
}

/** Whether a claim counts toward claimed and every live total, given the ids of the people on hold:
 *  only OPEN and SETTLED claims do, and a held person's OPEN claims do not. */
export function counts(c: Claim, held: ReadonlySet<number>): boolean {
  return c.status === 'SETTLED' || (c.status === 'OPEN' && !held.has(c.personId));
}

export function claimsKey(sheetId: string) {
  return ['sheet', sheetId, 'claims'];
}

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
  });
}

/** The sum of the amounts of those of these claims that count (given the ids of the people on hold),
 *  with two decimals (added up in cents, so no rounding drift). */
export function claimed(claims: Claim[], held: ReadonlySet<number>): string {
  const cents = claims.filter((c) => counts(c, held)).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
