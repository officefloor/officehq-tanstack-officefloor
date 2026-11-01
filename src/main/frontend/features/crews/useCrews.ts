import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

/** A crew as the server sends it: its name, its lead (the person and their name; null when no longer
 *  on the sheet) and its members in the order added. */
export type Crew = { name: string; leadId: number; lead: string | null; members: { personId: number; name: string }[] };

export const crewsKey = (sheet: string) => ['sheet', sheet, 'crews'] as const;

/** The sheet's crews, in the order added, re-read whenever the sheet is loaded. */
export function useCrews(sheet: string) {
  const view = useView();
  return useQuery({
    queryKey: viewKey(crewsKey(sheet), view),
    queryFn: () => getJson<Crew[]>(viewPath(`/api/sheets/${encodeURIComponent(sheet)}/crews`, view)),
    staleTime: 0,
    gcTime: 0,
  });
}

/** A crew's dues in cents: its lead's and its members' (null for n/a, or not on the sheet). */
export function crewDues(crew: Crew, perPerson: { person: { id: number }; due: bigint | null }[]) {
  const dueOf = (personId: number) => perPerson.find((p) => p.person.id === personId)?.due ?? null;
  return [dueOf(crew.leadId), ...crew.members.map((m) => dueOf(m.personId))];
}
