import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';
import { sum } from '../rates/rates';

/** A member of a crew: the person's `personId` and `name`. */
export type CrewMember = { personId: number; name: string };

/** A crew of the sheet: its `name` (trimmed, unique on the sheet), its lead (`leadId`, `lead` their name) and its
 *  members in the order added (never the lead, a person at most once). A person can be in several crews. */
export type Crew = { name: string; leadId: number; lead: string; members: CrewMember[] };

/** Under the people's key, so every change to a person (a rename or merge above all) reloads the crews. */
export const crewsKey = (sheetId: string) => [...peopleKey(sheetId), 'crews'];

/** The sheet's crews, in the order added. */
export function useCrews(sheetId: string) {
  return useQuery({
    queryKey: crewsKey(sheetId),
    queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`),
  });
}

/** A crew's due-total: its lead's and its members' due-totals (as their rows show them) added up, skipping n/a. */
export function crewDueTotal(crew: Crew, due: Map<number, string>): string {
  return sum([crew.leadId, ...crew.members.map((m) => m.personId)].map((id) => due.get(id) ?? 'n/a'));
}
