import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's approver: the user who may approve or reject their PENDING claims as a steward may. */
export interface Approver {
  personId: number;
  approver: string;
}

export const approversKey = (sheetId: string) => ['sheet', sheetId, 'approvers'] as const;

/** The approvers on the sheet, by person id. */
export function useApprovers(sheetId: string) {
  return useQuery({
    queryKey: approversKey(sheetId),
    queryFn: () => getJson<Approver[]>(`/api/sheets/${encodeURIComponent(sheetId)}/approvers`),
  });
}
