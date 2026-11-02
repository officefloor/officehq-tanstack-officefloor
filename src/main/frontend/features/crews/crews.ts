import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Person } from '../people/people';

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

/** A crew's due-total: the lead's and the members' due-totals added up, an n/a one (undefined) skipped. */
export function crewDueCents(crew: Crew, people: Person[], dueOf: (p: Person) => bigint | undefined): bigint {
  let due = 0n;
  for (const id of [crew.lead.person, ...crew.members.map((m) => m.person)]) {
    const person = people.find((p) => p.id === id);
    const cents = person ? dueOf(person) : undefined;
    if (cents !== undefined) due += cents;
  }
  return due;
}
