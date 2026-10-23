import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a crew: the person and their name. */
export type CrewMember = { personId: number; name: string };

/** A crew on the sheet: its name, its lead (a person) and its members in the order they were added. */
export type Crew = { id: number; name: string; leadId: number; lead: string; members: CrewMember[] };

export const crewsKey = (sheetId: string) => ['sheet', sheetId, 'crews'] as const;

/** The sheet's crews, in the order they were added. */
export function useCrews(sheetId: string) {
  return useQuery({
    queryKey: crewsKey(sheetId),
    queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`),
  });
}
