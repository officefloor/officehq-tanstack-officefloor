import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { nowKey, useReadNow } from '../../query/readNow';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

/** A grade a person has from a date (yyyy-MM-dd) until their next one's; a null `from` applies from the beginning of
 *  time (the first grade the person was given). */
export type PersonGrade = { grade: string; from: string | null };

/** A person on the sheet. `name` is unique on the sheet (case ignored), kept without outer spaces; `grades` are the
 *  grades recorded, by from-date, and `grade` (A, B or C) the one in force on the sheet's today.
 *  `version` goes up by one on each stored edit; an edit sends the version it was made from. `retiredFrom` (yyyy-MM-dd)
 *  is the date the person is retired from, null when not retired: a person with one counts as retired at once. `holds` are
 *  the person's holds, by from-date: a claim of theirs dated within one still counts in every total; a hold
 *  only takes the person's cut in the pools. `held` is whether they have any hold. `terms` are the person's own terms in days (ahead of
 *  their grade's and the sheet's when a claim is added) in force on the sheet's today, null when they have none;
 *  `ownTerms` are their own terms as recorded, by from-date (null: from the beginning of time), a claim having those in
 *  force on its date. `contact` is the person's contact
 *  (unique on the sheet; without surrounding spaces, lower-cased), null when none. `aliases` are the person's aliases,
 *  sorted as names are compared: no alias equals (case ignored) any name or alias on the sheet, and the quick-add bar and
 *  the paste box find a person by one as by the name. `clashing` is whether their name or an alias is used as a name or
 *  alias on another sheet of the sheet's tree (people added before the rule): every edit of them is refused until it
 *  renames them clear. `origin` is the sheet they were last transferred from, null when never transferred here.
 *  `retirements` are their past retirements (each ended by unretiring them), in the order they ended.
 *  `tags` are the person's tags (each trimmed and in lower case, no repeats, sorted; empty when none): a claim of theirs
 *  counts for each in the tag totals. */
/** A person's own terms in days (null: none) from a date (yyyy-MM-dd; null: the beginning of time). */
export type PersonTerms = { days: number | null; from: string | null };

export type Person = {
  id: number;
  sheetId: string;
  name: string;
  grade: string;
  version: number;
  retiredFrom: string | null;
  held: boolean;
  grades: PersonGrade[];
  holds: PersonHold[];
  terms: number | null;
  ownTerms: PersonTerms[];
  contact: string | null;
  aliases: string[];
  clashing: boolean;
  origin: string | null;
  retirements: PersonRetirement[];
  tags: string[];
  flag: PersonFlag | null;
};

/** A steward's flag on a person: its reason, in force until its until-date (yyyy-MM-dd, included). While the sheet's
 *  today is on or before it only a steward may open a claim of the person; after it the flag shows nothing. */
export type PersonFlag = { reason: string; until: string };

/** The reason of the person's flag in force on the day (yyyy-MM-dd), or '' when none is. */
export const flagOn = (person: Pick<Person, 'flag'>, on: string) =>
  person.flag && on !== '' && on <= person.flag.until ? person.flag.reason : '';

/** A past retirement of a person (yyyy-MM-dd): from their retirement date to the sheet's today when they were
 *  unretired. No claim of theirs may be dated after the from and before the to. */
export type PersonRetirement = { from: string; to: string };

/** A hold on a person from a date until a date (yyyy-MM-dd, both included); a null `to` is open-ended. */
export type PersonHold = { from: string; to: string | null };

/** Whether a hold of the person covers the date (yyyy-MM-dd). */
export const heldOn = (person: Pick<Person, 'holds'>, on: string) =>
  (person.holds ?? []).some((h) => h.from <= on && (h.to === null || on <= h.to));

/** Whether the person's retirement date is reached on the day (yyyy-MM-dd): it is on or before it. From then on their
 *  open (OPEN or PENDING) claims not bound by a statement count in no live total. */
export const retiredOn = (person: Pick<Person, 'retiredFrom'>, on: string) =>
  person.retiredFrom !== null && on !== '' && person.retiredFrom <= on;

/** The person's grade in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it. */
export function gradeOn(person: Pick<Person, 'grade' | 'grades'>, on: string): string {
  let found: PersonGrade | undefined;
  for (const g of person.grades ?? []) {
    if (g.from !== null && g.from > on) continue;
    if (found === undefined || found.from === null || (g.from !== null && g.from > found.from)) found = g;
  }
  return found?.grade ?? person.grade;
}

/** How the screen shows a person's name: `<name> (<grade in force on the sheet's today>)`. Exports, audit lines, the
 *  import and paste boxes and the selects of names keep the plain name. */
export const shownName = (person: Pick<Person, 'name' | 'grade'>) => `${person.name} (${person.grade})`;

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'];

/** The sheet's people, in list order (new people at the end; see the move buttons). */
export function usePeople(sheetId: string) {
  const now = useReadNow();
  return useQuery({
    queryKey: nowKey(peopleKey(sheetId), now),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheetId)}/people`, now),
  });
}

/** The id of the person whose editor is open (in the person form), or '' when the form adds. */
export const useEditingPerson = () => useSearchParam('person', asString);

/** Whether retired people are listed. Off when the screen is first opened; loading a sheet leaves it as it is. */
export const useShowRetired = () => useSearchParam('retired', asFlag);

/** What the people list is sorted by on this screen: list order (added), name (as names are compared: case ignored),
 *  grade, or due-total (n/a last). Equal keys keep list order. Only the screen's order changes: every action in list
 *  order, and the rollups, still go by list order. */
export const SORT_KEYS = ['added', 'name', 'grade', 'due-total'] as const;
export const useSortBy = () => useSearchParam('sort', asString);

/** The people in the order the sort key gives; `due` is each person's due-total (two decimals, or n/a) by id. */
export function sortPeople(people: Person[], key: string, due: Map<number, string>): Person[] {
  const nameKey = (p: Person) => p.name.toLowerCase();
  const dueKey = (p: Person) => {
    const d = due.get(p.id);
    return d === undefined || d === 'n/a' ? Infinity : Number(d);
  };
  const cmp: ((a: Person, b: Person) => number) | undefined =
    key === 'name'
      ? (a, b) => (nameKey(a) < nameKey(b) ? -1 : nameKey(a) > nameKey(b) ? 1 : 0)
      : key === 'grade'
        ? (a, b) => (a.grade < b.grade ? -1 : a.grade > b.grade ? 1 : 0)
        : key === 'due-total'
          ? (a, b) => (dueKey(a) === dueKey(b) ? 0 : dueKey(a) < dueKey(b) ? -1 : 1)
          : undefined;
  return cmp === undefined ? people : [...people].sort(cmp); // Array.sort is stable: equal keys keep list order
}
