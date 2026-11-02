import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person: `grade` is the one in force on the sheet's today, `grades` every grade they have been
 *  given, oldest first; `version` goes up by one each time they are saved. Their statements: how many have been
 *  taken, and the last one's due-total and date (null before the first); the date they are retired
 *  from (null when not retired); whether they are on hold (their OPEN claims then count in no live
 *  total). */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: GradeFrom[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retiredFrom: string | null;
  held: boolean;
};

/** One of a person's grades: in force from its from-date (yyyy-mm-dd; null for the first grade they
 *  were given, which applies from the beginning of time) until the next one's. */
export type GradeFrom = { grade: string; from: string | null };

/** The grade a person has on a date (yyyy-mm-dd): the one with the latest from-date on or before it. */
export function gradeOn(person: Person | undefined, on: string): string | undefined {
  let found: GradeFrom | undefined;
  for (const g of person?.grades ?? []) {
    if (g.from !== null && g.from > on) continue;
    if (found === undefined || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

/** One of a person's patrons: another person on the same sheet, by id. */
export type Patron = { personId: number; patronId: number };

export const GRADES = ['A', 'B', 'C'] as const;

export function peopleKey(sheetId: string) {
  return ['sheet', sheetId, 'people'];
}

/** The sheet's people and the patrons they have named, as the store holds them. */
type PeopleBody = { people: Person[]; patrons: Patron[] };

function peopleBody(sheetId: string): Promise<PeopleBody> {
  return getJson<PeopleBody>(`/api/sheets/${encodeURIComponent(sheetId)}/people`);
}

/** Read the sheet's people from the store. */
export async function fetchPeople(sheetId: string): Promise<Person[]> {
  return (await peopleBody(sheetId)).people;
}

export function patronsKey(sheetId: string) {
  return [...peopleKey(sheetId), 'patrons'];
}

/** Every patron named on the sheet, re-read from the store. */
export function usePatrons(sheetId: string) {
  return useQuery({
    queryKey: patronsKey(sheetId),
    queryFn: async () => (await peopleBody(sheetId)).patrons,
  });
}

/** The ids of those of these people who are on hold. */
export function heldIds(people: Person[] | undefined): Set<number> {
  return new Set((people ?? []).filter((p) => p.held).map((p) => p.id));
}

/** The sheet's people, in the order they were added. */
export function usePeople(sheetId: string) {
  return useQuery({
    queryKey: peopleKey(sheetId),
    queryFn: () => fetchPeople(sheetId),
  });
}
