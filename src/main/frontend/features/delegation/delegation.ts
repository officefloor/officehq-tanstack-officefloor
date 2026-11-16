import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The steward role delegated to a user by a steward for a from- and to-date (yyyy-MM-dd), both inclusive: while the
 *  sheet's today is inside the range the user acts as a steward, for the steward who delegated. */
export type Delegation = { id: number; sheetId: string; user: string; steward: string; from: string; to: string };

export const delegationsKey = (sheetId: string) => ['sheet', sheetId, 'delegations'];

/** The sheet's delegations, in the order they were made. */
export function useDelegations(sheetId: string) {
  return useQuery({
    queryKey: delegationsKey(sheetId),
    queryFn: () => getJson<Delegation[]>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`),
  });
}
