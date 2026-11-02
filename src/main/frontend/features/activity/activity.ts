import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const activityKey = (sheet: string, press: number) => ['activity', sheet, press] as const;

export const activityPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/activity`;

/** The sheet's activity lines ("<user> <EVENT> <count>", by user then event), read from the audit file at each press
 *  of the show button. Nothing is read until it is pressed. */
export function useActivity(sheet: string, press: number) {
  return useQuery({
    queryKey: activityKey(sheet, press),
    queryFn: () => getJson<string[]>(activityPath(sheet)),
    enabled: press > 0,
    gcTime: 0,
  });
}
