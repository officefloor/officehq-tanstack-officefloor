import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** One link: the person (`personId`) names another person on the sheet (`patronId`) as their patron. */
export type PatronLink = { personId: number; patronId: number };

/** Under the sheet's key, so re-loading the sheet re-reads its patrons. */
export const patronsKey = (sheetId: string) => [...sheetKey(sheetId), 'patrons'] as const;

export const patronsQuery = (sheetId: string) => ({
  queryKey: patronsKey(sheetId),
  queryFn: () => getJson<PatronLink[]>(`/api/sheets/${encodeURIComponent(sheetId)}/patrons`),
});

/** The ids of the person's patrons, in the order they were named (the store lists them so). */
export const patronIdsOf = (links: PatronLink[], personId: number) =>
  links.filter((l) => l.personId === personId).map((l) => l.patronId);

/**
 * The person's patrons with their shares (two decimals), in the order they were named: 100.00 split equally, each
 * share rounded half to even, and what that leaves over (100.00 less the shares added up) given to the first named.
 */
export function patronSharesOf(links: PatronLink[], personId: number): { patronId: number; share: string }[] {
  const ids = patronIdsOf(links, personId);
  if (ids.length === 0) return [];
  const n = BigInt(ids.length);
  const whole = 10000n / n;
  const rest = 10000n % n;
  const each = rest * 2n > n || (rest * 2n === n && whole % 2n === 1n) ? whole + 1n : whole;
  const first = 10000n - each * (n - 1n);
  const show = (h: bigint) => `${h / 100n}.${(h % 100n).toString().padStart(2, '0')}`;
  return ids.map((patronId, i) => ({ patronId, share: show(i === 0 ? first : each) }));
}
