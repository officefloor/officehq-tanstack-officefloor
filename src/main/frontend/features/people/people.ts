import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import type { Hold } from '../claims/claims';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

// `grade` is the grade in force on the sheet's today; `grades` every grade they were given with the date it
// applies from (null for the first, which applies from the beginning of time), by from-date.
// `version` is raised on every save; an edit sends the version it loaded and is refused (409) when
// someone else has saved the person since.
// `statementCount` is how many statements were taken for them; `statedTotal` and `statementDate` are the last
// one's due-total and the sheet's today when it was taken (null with none).
// `retiredFrom` is the date they are retired from (null: not retired); with one set they count as retired at
// once, whatever the date; from the day it is reached (the sheet's today on or after it) their OPEN and PENDING
// claims count in no live total, as a hold's would, unless a statement binds them.
// `held`: whether they have any hold. `holds`: their holds, earliest from-date first; their OPEN and PENDING
// claims dated within one count in no live total, unless a statement binds them; their other claims still count.
// `terms`: their own terms in days (null: none); a claim of theirs is due by its date plus these, ahead of their
// grade's terms and the sheet's.
// `contact`: their contact, trimmed and lower-cased (null: none); no two people on a sheet share one.
// `aliases`: other names they go by, sorted ignoring case; the quick-add bar and the paste box find them by one too.
// `clashing`: whether their name or an alias is in use on another sheet of the sheet's tree (its ancestors and
// descendants); an edit of them through the person form is refused until it leaves them no longer clashing.
// `patronIds`: the other people on the sheet they have named as patrons, in the order they were named.
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: DatedGrade[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retiredFrom: string | null;
  held: boolean;
  holds: Hold[];
  patronIds: number[];
  terms: number | null;
  contact: string | null;
  aliases: string[];
  clashing: boolean;
};

/** A grade a person was given, from a date on (YYYY-MM-DD; null: from the beginning of time). */
export type DatedGrade = { grade: string; from: string | null };

/** The grade in force for a person on a date: the one whose from-date is the latest on or before it, the first
 *  grade they were given applying from the beginning of time. */
export function gradeOn(person: Person, date: string | undefined): string {
  if (date === undefined) return person.grade;
  let found = person.grade;
  for (const g of person.grades) {
    if (g.from === null || g.from <= date) found = g.grade;
  }
  return found;
}

export const peopleQueryKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export const peoplePath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/people`;

/** The sheet's people, in the order they were added. */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleQueryKey(sheet ?? ''),
    queryFn: () => getJson<Person[]>(peoplePath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The id of the person whose editor is open (search param `editPerson`), or '' for none. */
/** The mutation key of a person form save; an editor is not opened while one is on its way, so it never loads
 *  the person from before the save (nor is closed by it). */
export const personSaveKey = ['person-save'] as const;

export const useEditingPerson = () => useSearchParam('editPerson', asString);

/** Whether retired people are listed (search param `showRetired`): off when the screen is first opened, and
 *  left as it is by loading a sheet. */
export const useShowRetired = () => useSearchParam('showRetired', asFlag);
