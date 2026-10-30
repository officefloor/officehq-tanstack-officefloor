import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const voidReasonsKey = (sheet: string) => ['sheet', sheet, 'void-reasons'] as const;

/** The sheet's void reasons, in the order added. */
export function useVoidReasons(sheet: string): string[] {
  const { data } = useQuery({
    queryKey: voidReasonsKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
