import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A member of a crew: a person, by id, and their name now. */
export type CrewMember = { personId: number; name: string };

/** A crew: its name, its lead (by id, and their name now) and its members in the order they were added. */
export type Crew = { name: string; leadId: number; lead: string; members: CrewMember[] };

export function crewsKey(sheetId: string) {
  return ['sheet', sheetId, 'crews'];
}

/** The sheet's crews, by name. */
export function useCrews(sheetId: string) {
  return useQuery({
    queryKey: crewsKey(sheetId),
    queryFn: async () => (await getJson<{ crews: Crew[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`)).crews,
  });
}
