import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Person } from '../people/people';
import { sumDue } from '../rates/rates';

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

/** A crew's due-total: its lead's and its members' due-totals (as shown on their rows) added up, skipping n/a.
 *  `due` is a person's due-total as shown on their row. */
export function crewDueTotal(crew: Crew, people: Person[], due: (person: Person) => string): string {
  const ids = new Set([crew.leadId, ...crew.members.map((m) => m.personId)]);
  return sumDue(people.filter((p) => ids.has(p.id)).map(due));
}
