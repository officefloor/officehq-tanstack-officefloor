import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Person } from '../people/people';
import { dueCents, hundredths, rateOf, type Rate } from '../rates/rates';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A status change a claim's row offers; the server refuses one its status does not allow. */
export type ClaimStatusChange = 'open' | 'settle' | 'void';

/** A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its number on
 * the sheet (given when it is opened; null while a draft; kept once voided), and its due fixed when it was
 * settled (two decimals; null while live: until settled, and when its grade had no rate then), and the
 * user who opened it (null until opened; that user may not settle it), and whether its person's last statement
 * bound it, with the figure it bound (null when its grade had no rate then). */
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
};

export const claimsKey = (sheet: string) => ['claims', sheet] as const;

export const claimsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet)),
  });
}

/** The claims that count toward claimed, due-total and every total: the OPEN and SETTLED ones, but not the OPEN
 *  ones of a person on hold. */
export function counted(claims: Claim[], people: Person[]): Claim[] {
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => c.status === 'SETTLED' || (c.status === 'OPEN' && !held.has(c.person)));
}

/** The sum of the claims' amounts with two decimals, added in whole cents so no float error shows. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => {
    const [whole, frac = ''] = c.amount.split('.');
    return sum + Number(whole) * 100 + Number(frac.padEnd(2, '0'));
  }, 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}

/** Whether a claim keeps its figure whatever the rates: fixed when settled, or bound by a statement. */
export function fixed(claim: Claim): boolean {
  return claim.settledDue !== null || claim.stated;
}

/** A claim's due: the one fixed when it was settled, else the one its statement bound, else its amount at the
 *  grade's rate in force on its date. */
export function claimDueCents(claim: Claim, pct: string | undefined): bigint | undefined {
  if (claim.settledDue !== null) return hundredths(claim.settledDue);
  if (claim.stated) return claim.statedDue === null ? undefined : hundredths(claim.statedDue);
  return dueCents(claim.amount, pct);
}


/** A person's due-total: the sum of their counted claims' rounded figures (the claims of others are skipped). n/a (undefined) when their grade
 *  has no rate on the sheet's today, unless a settled or stated claim keeps a figure: then the figures there are. */
export function personDueCents(
  claims: Claim[],
  rates: Rate[],
  person: Person,
  today: string | undefined,
): bigint | undefined {
  const mine = counted(claims.filter((c) => c.person === person.id), [person]);
  if (rateOf(rates, person.grade, today) === undefined && !mine.some(fixed)) return undefined;
  return mine.reduce((sum, c) => sum + (claimDueCents(c, rateOf(rates, person.grade, c.date)) ?? 0n), 0n);
}
