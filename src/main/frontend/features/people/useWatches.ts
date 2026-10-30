import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

export const watchesKey = (sheet: string, user: string, asOf = '') => ['sheet', sheet, 'watches', user, asOf] as const;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheet: string, user: string): string[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: watchesKey(sheet, user, asOf),
    queryFn: () =>
      getJson<string[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/watches/${encodeURIComponent(user)}`, asOf)),
    enabled: sheet !== '' && user !== '',
  });
  return data ?? [];
}
