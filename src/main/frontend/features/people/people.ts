import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asNumber, useSearchParam } from '../../url/useSearchParam';
import { sheetKey, useCurrentSheet } from '../session/session';

export const GRADES = ['A', 'B', 'C'] as const;

/** A grade recorded for a person, in force from its from-date (yyyy-MM-dd; null: from the beginning of time). */
export type GradeFrom = { grade: string; from: string | null };

/** The grade in force on the date (yyyy-MM-dd): the one recorded with the latest from-date on or before it. */
export function gradeOn(grades: GradeFrom[], date: string): string | undefined {
  let found: GradeFrom | undefined;
  for (const g of grades) {
    if (g.from !== null && g.from > date) continue;
    if (!found || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

/** A hold on a person, from its from-date to its to-date, both inclusive (yyyy-MM-dd; null: open-ended). */
export type Hold = { from: string | null; to: string | null };

/** Whether a hold covers the date (yyyy-MM-dd). */
export const covers = (h: Hold, date: string) => (h.from === null || h.from <= date) && (h.to === null || date <= h.to);

/**
 * The holds that apply to a person's live totals on the sheet's `today`: their own, and — from the day their
 * retirement date is reached — one covering every date, so their open and pending claims leave the totals as a
 * hold's would (settled ones, and ones a statement binds, still count).
 */
export const liveHolds = (p: { holds: Hold[]; retiredFrom: string | null }, today: string): Hold[] =>
  p.retiredFrom !== null && today !== '' && today >= p.retiredFrom ? [...p.holds, { from: null, to: null }] : p.holds;

/**
 * A person on the sheet as the store holds them; `grade` is the one in force on the sheet's today and
 * `grades` every grade recorded for them, each from its from-date; `version` counts their saves. `statementCount` counts
 * the statements taken of them; `statedTotal` is the last one's due-total and `statementDate` the
 * sheet's today when it was taken, both null before the first. `retired` is whether a retirement date is
 * set (whatever the date), `retiredFrom` that date, null while they are not retired. `held` is whether
 * they have any hold, `holds` each hold, oldest first: their open claims dated within one, and not bound by a
 * statement, count in no live total. `terms` is the person's own terms in days, null when they have none;
 * `contact` their contact (trimmed, lower-cased), null when they have none. `clashing` is whether another sheet of
 * the sheet's tree (its ancestors and descendants) uses their name or any of their aliases as a name or alias.
 */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: GradeFrom[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  held: boolean;
  holds: Hold[];
  terms: number | null;
  contact: string | null;
  clashing: boolean;
};

/**
 * The fields the person form submits; a blank `gradeFrom` is the sheet's today, blank `terms` none of their own,
 * blank `contact` none.
 */
export type PersonInput = { name: string; grade: string; gradeFrom: string; terms: string; contact: string };

/** The query key for the sheet's people; under sheetKey, so re-loading the sheet re-reads them. */
export const peopleKey = (sheet: string) => [...sheetKey(sheet), 'people'] as const;

/** The sheet's people, in their order in the list (new people go to the end; they can be moved up and down). */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: !!sheet,
  });
}

/** The id of the person whose editor is open in the person form, if any. */
export const useEditingPerson = () => useSearchParam('person', asNumber);

/** Whether retired people are listed; off until the toggle is turned on, and kept when a sheet is loaded. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
