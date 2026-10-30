import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const watchesKey = (sheet: string, user: string) => ['sheet', sheet, 'watches', user] as const;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheet: string, user: string): string[] {
  const { data } = useQuery({
    queryKey: watchesKey(sheet, user),
    queryFn: () =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/watches/${encodeURIComponent(user)}`),
    enabled: sheet !== '' && user !== '',
  });
  return data ?? [];
}
