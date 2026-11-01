import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** That `patron` is one of `person`'s patrons: both are ids of people on the sheet. `named` orders a person's patrons
 *  as they were named (lower first). */
export type Patron = { person: number; patron: number; named: number };

export const patronsKey = (sheet: string) => ['patrons', sheet] as const;

export const patronsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/patrons`;

/** Every patron named on the sheet. */
export function usePatrons(sheet: string) {
  return useQuery({
    queryKey: patronsKey(sheet),
    queryFn: () => getJson<Patron[]>(patronsPath(sheet)),
  });
}

/** Shares (in hundredths of a percent) for `count` patrons in the order they were named: 100.00 split equally, each
 *  rounded half to even to two decimals, with the remainder (100.00 minus the shares added up) on the first. */
export function shareCents(count: number): bigint[] {
  if (count === 0) return [];
  const n = BigInt(count);
  let each = 10000n / n;
  const twice = (10000n % n) * 2n;
  if (twice > n || (twice === n && each % 2n === 1n)) each += 1n;
  const shares = Array.from({ length: count }, () => each);
  shares[0] += 10000n - each * n;
  return shares;
}
