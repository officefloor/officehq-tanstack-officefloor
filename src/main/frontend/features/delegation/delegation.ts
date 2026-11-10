import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/**
 * The steward role delegated by a steward (`by`) to a user from a date to a date (yyyy-MM-dd), inclusive,
 * by the sheet's today: while today is inside the range the user acts as a steward.
 */
export type Delegation = { id: number; user: string; by: string; from: string; to: string };

/** A delegation as the screen sends it (a blank from-date: the sheet's today; a blank to-date: the from-date). */
export type DelegationChange = { user: string; from: string; to: string };

export const delegationsKey = (sheetId: string) => ['sheet', sheetId, 'delegations'] as const;

/** The delegations made on the sheet, in the order they were made. */
export function useDelegations(sheetId: string) {
  return useQuery({
    queryKey: delegationsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ delegations: Delegation[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`))
        .delegations,
  });
}

/** A from- or to-date as the screen takes it: blank, or a date yyyy-MM-dd. */
export const DATE = /^\d{4}-\d{2}-\d{2}$/;
