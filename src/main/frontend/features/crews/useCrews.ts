import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Person } from '../people/usePeople';

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

/** A crew's due-total in cents: the lead's and the members' due-totals added up, n/a skipped. */
export function crewDueCents(crew: Crew, byId: Map<string, Person>, totalOf: (person: Person) => bigint | null): bigint {
  return [crew.leadId, ...crew.members.map((m) => m.personId)].reduce((sum, id) => {
    const person = byId.get(id);
    const due = person === undefined ? null : totalOf(person);
    return due === null ? sum : sum + due;
  }, 0n);
}
