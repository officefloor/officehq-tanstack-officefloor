import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `version` goes up on every save; an editor sends back the version it loaded, so a save over
 *  someone else's newer one is refused. `statementCount` is how many statements were taken;
 *  `statedTotal` is the last one's due-total ("n/a" possible, "" when none) and `statementDate` the
 *  sheet's today when it was taken ("" when none). `retired` is whether a retirement date is set
 *  (retired at once, whatever the date) and `retiredFrom` is that date ("" when none). `held` is
 *  whether the person has any hold and `holds` lists each (from, to; to "" when open-ended): their
 *  OPEN claims dated within one, unless bound by a statement, count in no live total. `patrons` are the
 *  names of the people they named as patrons, sorted, and `patronIds` those people's ids. `grade`
 *  is the grade in force on the sheet's today when loaded and `grades` every grade recorded. */
export type Person = {
  id: string;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string;
  statementDate: string;
  retired: boolean;
  retiredFrom: string;
  held: boolean;
  holds: HoldFrom[];
  patrons: string[];
  patronIds: string[];
  grades: GradeFrom[];
};

/** A hold from a date to a date, both included (YYYY-MM-DD; to "" when open-ended). */
export type HoldFrom = { from: string; to: string };

/** Whether one of the person's holds covers the date. */
export const heldOn = (person: Person, date: string) =>
  person.holds.some((h) => h.from <= date && (h.to === '' || date <= h.to));

/** A grade in force from a date (YYYY-MM-DD; "" for the beginning of time). */
export type GradeFrom = { from: string; grade: string };

/** The person's grade in force on a date: the one with the latest from-date on or before it (with
 *  no date yet, the one in force on the sheet's today as loaded). */
export function gradeOn(person: Person, date: string | undefined): string {
  if (date === undefined) return person.grade;
  let found: GradeFrom | undefined;
  for (const g of person.grades) {
    if (g.from <= date && (found === undefined || g.from >= found.from)) found = g;
  }
  return found?.grade ?? person.grade;
}

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheet: string): Person[] {
  const { data } = useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
