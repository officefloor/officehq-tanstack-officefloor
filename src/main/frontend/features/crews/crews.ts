import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A person of a crew: their id and name. */
export type CrewPerson = { id: number; name: string };

/** A sheet's crew as the store holds it: a trimmed name, on the sheet at most once, its lead (a person of the sheet)
 *  and its members, each a person of the sheet at most once and never the lead, in the order they were added. */
export type Crew = { id: number; name: string; lead: CrewPerson | null; members: CrewPerson[] };

export const crewsQueryKey = (sheetId: string) => ['sheet', sheetId, 'crews'] as const;

export const crewsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/crews`;

/** The sheet's crews, in the order they were added. */
export function useCrews() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: crewsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Crew[]>(crewsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
