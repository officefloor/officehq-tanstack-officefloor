import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's statements: how many were taken, and the last one's due-total (two decimals, or n/a) and date
 *  (yyyy-MM-dd, the sheet's today when it was taken). A person with none is absent. */
export type StatementSummary = { person: number; count: number; lastDueTotal: string; lastDate: string };

export const statementsKey = (sheet: string) => ['statements', sheet] as const;

export const statementsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/statements`;

/** The statements taken on the sheet, per person. */
export function useStatements(sheet: string) {
  return useQuery({
    queryKey: statementsKey(sheet),
    queryFn: () => getJson<StatementSummary[]>(statementsPath(sheet)),
  });
}
