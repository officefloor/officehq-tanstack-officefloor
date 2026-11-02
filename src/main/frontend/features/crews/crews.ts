import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person in a crew: their id and name. */
export type CrewMember = { person: number; name: string };

/** A crew: its lead and its members (the lead not among them), in the order added. */
export type Crew = { name: string; lead: CrewMember; members: CrewMember[] };

export const crewsKey = (sheet: string) => ['crews', sheet] as const;

export const crewsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/crews`;

export const crewMembersPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/crew-members`;

/** The sheet's crews, in the order added. */
export function useCrews(sheet: string) {
  return useQuery({
    queryKey: crewsKey(sheet),
    queryFn: () => getJson<Crew[]>(crewsPath(sheet)),
  });
}
