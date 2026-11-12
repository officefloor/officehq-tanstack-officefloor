import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** PENDING: settled by a clerk above the clerk limit, awaiting a steward's approval; handled as OPEN,
 *  except that a statement does not bind it while it is PENDING. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** `amount` has two decimals; `date` is yyyy-MM-dd; `number` is given when the claim is opened
 *  (null for a draft or a voided claim): the sheet-wide 1, 2, ... or, numbered per period, <period>-<n> / X-<n>. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  /** The due fixed when the claim was settled; null when it has none, so its due follows the rate. */
  due: string | null;
  /** The user who opened the claim; null until it is opened. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim to its figure then. */
  stated: boolean;
  /** The figure the claim was bound to; null when not bound, or bound while it had none. */
  statedDue: string | null;
  /** The number of the settled claim a correction (a negative amount) corrects; null for any other claim. */
  corrects: string | null;
  /** The date (yyyy-MM-dd) its live figures round by: its own, or for a correction that of the claim it corrects. */
  roundsBy: string;
  /** The date (yyyy-MM-dd) it is due by, fixed when it was added from the terms of its person's grade then. */
  dueBy: string;
  /** Its status log, oldest first: `<STATUS> by <user> on <date> due=<due>`, one line per status it has had. */
  log: string[];
};

/** A correction: a claim with a negative amount, which has no floor and no late charge. */
export const isCorrection = (c: Claim) => c.amount.trim().startsWith('-') && Number(c.amount) !== 0;

/** Whether the claim is open: OPEN, or PENDING, which is handled as open. */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/** Only OPEN, PENDING and SETTLED claims count toward claimed and the totals. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** Whether the claim keeps the figure its person's last statement bound it to: not while PENDING. */
export const isBound = (c: Claim) => c.stated && c.statedDue !== null && c.status !== 'PENDING';

/** These claims less the OPEN (or PENDING) claims of people on hold, which count in no live total. */
export const live = (claims: Claim[], people: { id: number; held: boolean }[] | undefined) => {
  const held = new Set((people ?? []).filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => !(isOpen(c) && held.has(c.personId)));
};

/** Under the sheet's key, so loading the sheet again re-reads its claims. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: sheet !== '',
  });
}

/** Amounts are added in whole cents, so the sum is exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** The sum of the amounts of those of these claims that count, with two decimals. */
export function claimed(claims: Claim[]): string {
  return (claims.filter(counts).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}

/** The ids of the claims selected for a bulk action, kept in the URL (`selectedClaims`, comma separated). */
export function useSelectedClaims(): [number[], (ids: number[]) => void] {
  const [raw, setRaw] = useSearchParam('selectedClaims', asString);
  const ids = raw.split(',').filter((s) => s !== '').map(Number).filter((n) => !Number.isNaN(n));
  return [ids, (next) => setRaw(next.join(','))];
}
