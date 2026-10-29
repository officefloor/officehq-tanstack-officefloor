import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** A person's approver: the user who may approve or reject their PENDING claims as a steward may. */
export type Approver = { personId: number; user: string };

/** Under the sheet's key, so re-loading the sheet re-reads its approvers. */
export const approversKey = (sheetId: string) => [...sheetKey(sheetId), 'approvers'] as const;

export const approversQuery = (sheetId: string) => ({
  queryKey: approversKey(sheetId),
  queryFn: () => getJson<Approver[]>(`/api/sheets/${encodeURIComponent(sheetId)}/approvers`),
});
