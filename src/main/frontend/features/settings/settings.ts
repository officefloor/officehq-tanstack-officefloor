import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** The sheet's settings: the clerk limit with two decimals, the most due a clerk may settle ('' for no limit). */
export type Settings = { clerkLimit: string };

/** A setting's key as the server names it, with the field of {@link Settings} that shows it. */
export type SettingKey = 'clerk-limit';

export const settingsKey = (sheet: string) => ['settings', sheet] as const;

export const settingsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/settings`;

export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(settingsPath(sheet)),
  });
}
