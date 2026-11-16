import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's statements: how many were taken, the last one's due-total (two decimals, or n/a) and date
 *  (yyyy-MM-dd, the sheet's today when it was taken), and one line per statement in force, oldest first, as
 *  "<statement date> <due-total>". A person with none has no entry. */
export type StatementSummary = { personId: number; count: number; dueTotal: string; date: string; lines: string[] };

export const statementsKey = (sheetId: string) => ['sheet', sheetId, 'statements'];

/** Each person's statements on the sheet. */
export function useStatements(sheetId: string) {
  return useQuery({
    queryKey: statementsKey(sheetId),
    queryFn: () => getJson<StatementSummary[]>(`/api/sheets/${encodeURIComponent(sheetId)}/statements`),
  });
}
