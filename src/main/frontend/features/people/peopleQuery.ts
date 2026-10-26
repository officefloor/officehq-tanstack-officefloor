import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/**
 * `version` is raised on every edit; an edit names the version it loaded. `retiredFrom` (yyyy-mm-dd) is
 * set once the person is retired, which they are at once, whatever the date; null otherwise. `held` is whether
 * they have any hold; `holds` are their holds by from-date: their OPEN claims dated within one, unless bound by a
 * statement, count in no live total. `terms` are their own terms in days, which come before those of their grade
 * and of the sheet; null for none.
 */
export type Person = { id: number; name: string; grade: Grade; grades: Graded[]; version: number; retiredFrom: string | null; held: boolean; holds: Held[]; terms: number | null };

/** A hold from `from` to `to` inclusive (yyyy-mm-dd); `to` is null when it has no end. */
export type Held = { from: string; to: string | null };

/** Whether the date (yyyy-mm-dd) falls within any of the person's holds. */
export function heldOn(person: Person | undefined, date: string): boolean {
  return (person?.holds ?? []).some((h) => h.from <= date && (h.to === null || date <= h.to));
}

/** A grade a person was given from `from` (yyyy-mm-dd) on; null for the first, from the beginning of time. */
export type Graded = { from: string | null; grade: Grade };

/** The person's grade in force on the date (yyyy-mm-dd): the one with the latest from-date on or before it. */
export function gradeOn(person: Person | undefined, date: string): Grade | undefined {
  if (!person) return undefined;
  // No from-date sorts as '', before every date.
  const applying = person.grades.filter((g) => (g.from ?? '') <= date);
  return applying.reduce<Graded | undefined>((best, g) => (!best || (g.from ?? '') > (best.from ?? '') ? g : best), undefined)
    ?.grade ?? person.grade;
}

/** Under the sheet's key, so re-loading the sheet re-reads its people. */
export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
});
