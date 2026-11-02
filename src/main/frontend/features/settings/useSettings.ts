import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The role a user name holds on the sheet: clerk or steward. A name not listed is a clerk. */
export type Role = { user: string; role: string };

/** The sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be), its terms (the days after a claim's date that it is due by) and the
 *  roles set. */
export type Settings = { clerkLimit: string | null; floor: string; terms: number; roles: Role[] };

export function settingsKey(sheetId: string) {
  return ['sheet', sheetId, 'settings'];
}

export function useSettings(sheetId: string) {
  return useQuery({
    queryKey: settingsKey(sheetId),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`),
  });
}
