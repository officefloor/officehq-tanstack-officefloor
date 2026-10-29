import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/**
 * `version` is raised on every edit; an edit names the version it loaded. `retiredFrom` (yyyy-mm-dd) is
 * set once the person is retired, which they are at once, whatever the date; null otherwise. `held` is whether
 * they have any hold; `holds` are their holds by from-date: their claims still count in every total, but while
 * held on the sheet's today they get no cut in a pool. `terms` are their own terms in days in force on the sheet's today, which come
 * before those of their grade and of the sheet; null for none. `termses` are all their own terms recorded, by from-date
 * (null for those from the beginning of time); a claim is due by the ones in force on its date. `contact` is their contact, trimmed and lower-cased, unique on the sheet; null
 * for none. `aliases` are their aliases, sorted as names are compared (ignoring case); the quick-add bar and the
 * paste box find a person by one as well as by the name. A name or alias is unique across the sheet's tree (its
 * ancestors and descendants); `clashing` is whether the person's name or an alias is used by someone else there all
 * the same (they were on the sheet before), which refuses every edit of them until a rename frees them. `origin` is
 * the id of the sheet they were transferred from; null when they were not transferred in. `retirements` are their
 * past retirements by from-date, each ended by unretiring them: a claim dated strictly between its dates is refused.
 * `tags` are the tags they carry, trimmed, lower-cased, without repeats and sorted; a tag total counts each of their
 * claims for them. `snapshots` are the SNAPSHOTs written for them, oldest first.
 */
export type Person = { id: number; name: string; grade: Grade; grades: Graded[]; version: number; retiredFrom: string | null; held: boolean; holds: Held[]; terms: number | null; contact: string | null; aliases: string[]; clashing: boolean; origin: string | null; retirements: Retired[]; tags: string[]; snapshots: Snapshotted[]; termses: Termed[] };

/** A person's own terms recorded from `from` (yyyy-mm-dd; null for the beginning of time) on; `terms` null for none. */
export type Termed = { from: string | null; terms: number | null };

/** A SNAPSHOT written for a person: `day` is the day being left (yyyy-mm-dd), `dueTotal` their due-total then ("n/a" for none). */
export type Snapshotted = { day: string; dueTotal: string };

/** A past retirement from `from` to `to`, the day they were unretired (yyyy-mm-dd). */
export type Retired = { from: string; to: string };

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

/** The person's name as the screen shows it: `<name> (<grade in force on the date>)`. */
export function shownName(person: Person, date: string): string {
  return `${person.name} (${gradeOn(person, date)})`;
}

/** Under the sheet's key, so re-loading the sheet re-reads its people. */
export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`),
});
