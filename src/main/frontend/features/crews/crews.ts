import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';

/** A person in a crew (never its lead). */
export type CrewMember = { personId: string; name: string };

/** A crew on the sheet: its name, its lead (a person on the sheet) and its members in the order added. */
export type Crew = { name: string; leadId: string; lead: string; members: CrewMember[] };

// Under the people's key: a change to a person (a new name) reads the crews again too.
export const crewsKey = (sheetId: string) => [...peopleKey(sheetId), 'crews'] as const;

/** The crews on the sheet, by name. */
export function useCrews(sheetId: string) {
  return useQuery({
    queryKey: crewsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ crews: Crew[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`)).crews,
  });
}
