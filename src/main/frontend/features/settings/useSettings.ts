import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role a user name holds on the sheet: clerk (any name not listed) or steward. */
export type Role = { user: string; role: string };

/** The sheet's settings: the clerk limit with two decimals ("" for no limit), the floor with two
 *  decimals (a claim's live due is at least it) and the roles set. */
export type Settings = { clerkLimit: string; floor: string; roles: Role[] };

/** The floor a sheet starts with, used until its settings have loaded. */
export const DEFAULT_FLOOR = '1.00';

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

export function useSettings(sheet: string): Settings | undefined {
  const { data } = useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: sheet !== '',
  });
  return data;
}

/** The sheet's floor. */
export function useFloor(sheet: string): string {
  return useSettings(sheet)?.floor ?? DEFAULT_FLOOR;
}
