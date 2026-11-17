import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** That `user` is the approver of the person `personId`: they may approve or reject the person's PENDING claims as a
 *  steward may. */
export type Approver = { personId: number; user: string };

export const approversKey = (sheetId: string) => ['sheet', sheetId, 'approvers'];

/** Every person's approver on the sheet. */
export function useApprovers(sheetId: string) {
  return useQuery({
    queryKey: approversKey(sheetId),
    queryFn: () => getJson<Approver[]>(`/api/sheets/${encodeURIComponent(sheetId)}/approvers`),
  });
}
