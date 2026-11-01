import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { byUser } from '../session/session';

export const watchesKey = (sheet: string, user: string) => ['watches', sheet, user] as const;

export const watchesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/watches`;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheet: string, user: string) {
  return useQuery({
    queryKey: watchesKey(sheet, user),
    queryFn: () => getJson<number[]>(byUser(watchesPath(sheet), user)),
  });
}
