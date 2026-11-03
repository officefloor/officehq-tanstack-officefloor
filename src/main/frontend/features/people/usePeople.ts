import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useReader } from '../../api/readNow';

/** A person: `grade` is the one in force on the sheet's today, `grades` every grade they have been
 *  given, oldest first; `version` goes up by one each time they are saved. Their statements: how many have been
 *  taken, and the last one's due-total and date (null before the first); the date they are retired
 *  from (null when not retired); their holds, oldest first, and whether they have any (their open (OPEN or PENDING)
 *  claims dated within a hold, unless bound by a statement, then count in no live total); their own terms in
 *  days (null when they have none: their grade's, else the sheet's, apply); their contact (null when they have none);
 *  the sheet they were transferred from (null when they were not transferred here). */
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
  holds: Hold[];
  terms: number | null;
  contact: string | null;
  origin: string | null;
};

/** One of a person's holds: from its from-date to its to-date (yyyy-mm-dd, both inclusive; a null
 *  from-date is the beginning of time, a null to-date open-ended). */
export type Hold = { from: string | null; to: string | null };

/** Whether any of these holds covers a date (yyyy-mm-dd). */
export function heldOn(holds: readonly Hold[] | undefined, on: string): boolean {
  return (holds ?? []).some((h) => (h.from === null || h.from <= on) && (h.to === null || h.to >= on));
}

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

/** One of a person's aliases: another name they are known by on the sheet. */
export type Alias = { personId: number; alias: string };

export const GRADES = ['A', 'B', 'C'] as const;

export function peopleKey(sheetId: string) {
  return ['sheet', sheetId, 'people'];
}

/** The sheet's people, the patrons they have named, the people the signed-in user watches, their aliases and the
 *  people whose name or an alias is already a name or alias on another sheet of the tree, as the store holds them. */
type PeopleBody = { people: Person[]; patrons: Patron[]; watched: number[]; aliases: Alias[]; clashing: number[] };

function peopleBody(sheetId: string, get: <T>(path: string) => Promise<T> = getJson): Promise<PeopleBody> {
  return get<PeopleBody>(`/api/sheets/${encodeURIComponent(sheetId)}/people`);
}

/** Read the sheet's people from the store. */
export async function fetchPeople(sheetId: string, get?: <T>(path: string) => Promise<T>): Promise<Person[]> {
  return (await peopleBody(sheetId, get)).people;
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

export function aliasesKey(sheetId: string) {
  return [...peopleKey(sheetId), 'aliases'];
}

/** Every alias on the sheet, re-read from the store. */
export function useAliases(sheetId: string) {
  return useQuery({
    queryKey: aliasesKey(sheetId),
    queryFn: async () => (await peopleBody(sheetId)).aliases ?? [],
  });
}

export function clashingKey(sheetId: string) {
  return [...peopleKey(sheetId), 'clashing'];
}

/** The ids of the people whose name or an alias is a name or alias on another sheet of the sheet's tree. */
export function useClashing(sheetId: string) {
  return useQuery({
    queryKey: clashingKey(sheetId),
    queryFn: async () => (await peopleBody(sheetId)).clashing ?? [],
  });
}

export function watchedKey(sheetId: string) {
  return [...peopleKey(sheetId), 'watched'];
}

/** The ids of the people the signed-in user watches, re-read from the store. */
export function useWatched(sheetId: string) {
  return useQuery({
    queryKey: watchedKey(sheetId),
    queryFn: async () => (await peopleBody(sheetId)).watched ?? [],
  });
}

/** These people's holds, by person id, as they act on live totals on the sheet's today: a person whose
 *  retirement date is reached (the sheet's today on or after it) is held over every date as well. */
export function holdsById(people: Person[] | undefined, today: string | undefined): Map<number, Hold[]> {
  return new Map((people ?? []).map((p) => [p.id, retiredOn(p, today)
    ? [...(p.holds ?? []), { from: null, to: null }] : p.holds ?? []]));
}

/** Whether a person's retirement date is reached on the sheet's today (unknown until it is loaded). */
export function retiredOn(person: Person, today: string | undefined): boolean {
  return person.retiredFrom !== null && today !== undefined && person.retiredFrom <= today;
}

/** The sheet's people, in list order. */
export function usePeople(sheetId: string) {
  const read = useReader();
  return useQuery({
    queryKey: read.key(peopleKey(sheetId)),
    queryFn: () => fetchPeople(sheetId, read.get),
  });
}
