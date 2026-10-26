import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/**
 * A person's statements: how many were taken, and the last one's due-total ("n/a" when it had none) and
 * date (yyyy-mm-dd), both null when none was taken. A person with no statement has no entry.
 */
export type Statements = { personId: number; count: number; dueTotal: string | null; date: string | null };

/** Under the sheet's key, so re-loading the sheet re-reads its statements. */
export const statementsKey = (sheetId: string) => [...sheetKey(sheetId), 'statements'] as const;

export const statementsQuery = (sheetId: string) => ({
  queryKey: statementsKey(sheetId),
  queryFn: () => getJson<Statements[]>(`/api/sheets/${encodeURIComponent(sheetId)}/statements`),
});
