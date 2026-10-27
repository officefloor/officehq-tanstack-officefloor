import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claimsQuery';

/** A claim template of the sheet: its name, its amount with two decimals and its note. */
export type Template = { name: string; amount: string; note: string };

/** Under the claims' key, so re-loading the sheet re-reads the list. */
export const templatesKey = (sheetId: string) => [...claimsKey(sheetId), 'templates'] as const;

export const templatesQuery = (sheetId: string) => ({
  queryKey: templatesKey(sheetId),
  queryFn: () => getJson<Template[]>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`),
});
