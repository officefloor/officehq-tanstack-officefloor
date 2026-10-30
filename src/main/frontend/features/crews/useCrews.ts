import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A crew: its name, its lead and its members in the order added (each a person's id and name). */
export type Crew = { name: string; leadId: string; leadName: string; members: { personId: string; name: string }[] };

export const crewsKey = (sheet: string) => ['sheet', sheet, 'crews'] as const;

/** The sheet's crews, in the order added. */
export function useCrews(sheet: string): Crew[] {
  const { data } = useQuery({
    queryKey: crewsKey(sheet),
    queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
