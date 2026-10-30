import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const descendantsKey = (sheet: string) => ['sheet', sheet, 'descendants'] as const;

/** The sheet's descendants: the sheets naming it as their parent, their children and so on. */
export function useDescendants(sheet: string): string[] {
  const { data } = useQuery({
    queryKey: descendantsKey(sheet),
    queryFn: () => getJson<{ sheets: string[] }>(`/api/sheets/${encodeURIComponent(sheet)}/descendants`),
    enabled: sheet !== '',
  });
  return data?.sheets ?? [];
}
