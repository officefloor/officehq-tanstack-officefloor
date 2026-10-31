import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `version` counts the person's saves; an edit sends the version it loaded, so a save made by
 *  someone else in the meantime refuses it. `statementCount` is how many statements have been
 *  taken for them; `statedTotal` the last one's due-total (two decimals, null when it was n/a or
 *  none was taken) and `statementDate` its date (yyyy-mm-dd, null when none was taken).
 *  `retired` is whether a retirement date is set (whatever the date), `retiredFrom` that date
 *  (yyyy-mm-dd, null when not retired). `held` is whether they have any hold; `holds` every hold, in the
 *  order added, from a date to a date (both inclusive; `to` null: open-ended). Their OPEN claims
 *  dated within a hold count in no live total, unless a statement binds them. `patrons` are the ids of the people they have named as patrons, in
 *  the order they were named. `grade` is the grade in force on the sheet's today when they were read;
 *  `grades` every grade recorded, by from-date (null: from the beginning of time, first). */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: { grade: string; from: string | null }[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  held: boolean;
  holds: { from: string; to: string | null }[];
  patrons: number[];
};

/** Whether `date` (yyyy-mm-dd) falls within any of the person's holds. */
export function heldOn(person: Person, date: string): boolean {
  return person.holds.some((h) => date >= h.from && (h.to === null || date <= h.to));
}

/** By person id, whether a date falls within one of that person's holds. */
export function heldOnBy(people: Person[]): (personId: number, date: string) => boolean {
  const byId = new Map(people.map((p) => [p.id, p]));
  return (personId, date) => {
    const p = byId.get(personId);
    return p !== undefined && heldOn(p, date);
  };
}

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people in the order they were added, re-read whenever the sheet is loaded. */
export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** The person's grade in force on `date` (yyyy-mm-dd): the one from the latest date on or before
 *  it, the first one they were given applying from the beginning of time. */
export function gradeOn(person: Person, date: string): string {
  let found = person.grades[0]?.grade ?? person.grade;
  for (const g of person.grades) if (g.from === null || g.from <= date) found = g.grade;
  return found;
}
