import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A role a user name holds on the sheet: clerk (any name not listed) or steward. */
export type Role = { user: string; role: string };

/** The sheet's settings: the clerk limit with two decimals ("" for no limit) and the roles set. */
export type Settings = { clerkLimit: string; roles: Role[] };

export const settingsKey = (sheet: string) => ['sheet', sheet, 'settings'] as const;

export function useSettings(sheet: string): Settings | undefined {
  const { data } = useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`),
    enabled: sheet !== '',
  });
  return data;
}
