import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

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
  /** The person's own terms in days; "" for none. */
  terms: string;
  /** The person's contact, trimmed and lower-cased; "" for none. */
  contact: string;
  /** The person's aliases, sorted as names are compared. */
  aliases: string[];
};

/** A hold from a date to a date, both included (YYYY-MM-DD; to "" when open-ended). */
export type HoldFrom = { from: string; to: string };

/** Whether one of the person's holds covers the date. */
/** Whether the person's retirement date is reached on the sheet's today: from then their OPEN and
 *  PENDING claims, unless bound by a statement, count in no live total. */
export const retiredOn = (person: Person, today: string | undefined) =>
  person.retiredFrom !== '' && today !== undefined && today >= person.retiredFrom;

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

export const peopleKey = (sheet: string, asOf = '') => ['sheet', sheet, 'people', asOf] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheet: string): Person[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: peopleKey(sheet, asOf),
    queryFn: () => getJson<Person[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/people`, asOf)),
    enabled: sheet !== '',
  });
  return data ?? [];
}
