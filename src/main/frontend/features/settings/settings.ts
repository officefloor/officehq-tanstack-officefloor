import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { hundredths } from '../rates/rates';

/** The sheet's settings: the clerk limit with two decimals, the most due a clerk may settle ('' for no limit);
 *  and the floor with two decimals, the least a claim's live due is. */
export type Settings = { clerkLimit: string; floor: string };

/** A setting's key as the server names it, with the field of {@link Settings} that shows it. */
export type SettingKey = 'clerk-limit' | 'floor';

/** The floor a sheet starts with. */
export const DEFAULT_FLOOR = '1.00';

export const settingsKey = (sheet: string) => ['settings', sheet] as const;

export const settingsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/settings`;

export function useSettings(sheet: string) {
  return useQuery({
    queryKey: settingsKey(sheet),
    queryFn: () => getJson<Settings>(settingsPath(sheet)),
  });
}

/** The sheet's floor in hundredths: the least a claim's live due is. */
export function useFloorCents(sheet: string): bigint {
  const { data } = useSettings(sheet);
  return hundredths(data?.floor ?? DEFAULT_FLOOR);
}
