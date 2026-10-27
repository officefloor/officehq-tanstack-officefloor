import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** The ids of the sheet's descendants: the sheets naming it as their parent, their children and so on (the sheet itself is not among them). */
export const descendantsKey = (sheetId: string) => [...sheetKey(sheetId), 'descendants'] as const;

export const descendantsQuery = (sheetId: string) => ({
  queryKey: descendantsKey(sheetId),
  queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/descendants`),
});
