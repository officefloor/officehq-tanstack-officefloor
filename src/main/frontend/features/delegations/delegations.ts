import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A steward (by) lends the steward role to a user from a from-date to a to-date, both inclusive, by the sheet's
 *  today. While today is inside the range the user acts as a steward in every rule. */
export type Delegation = { id: number; by: string; user: string; from: string; to: string };

export const delegationsKey = (sheet: string) => ['delegations', sheet] as const;

export const delegationsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/delegations`;

/** Every delegation made on the sheet, oldest first. */
export function useDelegations(sheet: string) {
  return useQuery({
    queryKey: delegationsKey(sheet),
    queryFn: () => getJson<Delegation[]>(delegationsPath(sheet)),
  });
}
