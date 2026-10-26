import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** The steward role delegated to `user` by the steward `by`, from `from` to `to` (yyyy-mm-dd, both inclusive). */
export type Delegation = { user: string; from: string; to: string; by: string };

/** Under the sheet's key, so re-loading the sheet re-reads its delegations. */
export const delegationsKey = (sheetId: string) => [...sheetKey(sheetId), 'delegations'] as const;

export const delegationsQuery = (sheetId: string) => ({
  queryKey: delegationsKey(sheetId),
  queryFn: () => getJson<Delegation[]>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`),
});
