import { useQuery } from '@tanstack/react-query';
import { useReadScope } from '../../query/live';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

export type Grade = 'A' | 'B' | 'C';
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** One of a person's grades, in force from its from-date (yyyy-MM-dd; null when it applies from the beginning of time). */
export type GradeFrom = { grade: Grade; from: string | null };
/** A person's own terms (days; null: none) from the from-date (null: from the beginning of time). */
export type TermsFrom = { terms: number | null; from: string | null };

// grade is the one in force on the sheet's today; grades are every one recorded, by from-date.
// retiredFrom is null unless the person is retired; once set, they count as retired whatever the date.
/** One of a person's holds: from its from-date to its to-date, both included (yyyy-MM-dd; to is null when it never ends). */
export type Hold = { from: string; to: string | null };

/** One of a person's past retirements: from its retired-from date to the day they were unretired (yyyy-MM-dd). */
export type Retirement = { from: string; to: string };

// holds are every one added, in that order; their OPEN claims dated within a hold count in no live total, unless
// bound by a statement; their other claims still do.
export type Person = {
  id: number;
  name: string;
  grade: Grade;
  version: number;
  retiredFrom: string | null;
  holds: Hold[];
  grades: GradeFrom[];
  /** The person's own terms in force on the sheet's today, in days, ahead of their grade's and the sheet's; null when none. */
  terms: number | null;
  /** Their own terms by from-date, the ones from the beginning of time first. */
  termsFrom: TermsFrom[];
  /** Whether the sheet's today is on or after retiredFrom: from then their OPEN and PENDING claims, unless bound by a
   *  statement, count in no live total. */
  retiredNow: boolean;
  /** The person's contact, trimmed and lower-cased; null when they have none. */
  contact: string | null;
  /** Whether their name or an alias is already a name or alias on another sheet of the sheet's tree (its ancestors
   *  and descendants): every edit of them is refused until it renames them out of the clash. */
  clashing: boolean;
  /** The sheet they were transferred from; null for anyone not transferred. */
  origin: string | null;
  /** Their past retirements, each ended by unretiring them, in the order they were unretired. */
  retirements: Retirement[];
  /** Their tags: each trimmed and lower-cased, repeats dropped, sorted; a claim of theirs counts in each tag's due-total. */
  tags: string[];
};

/** The person's grade in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it. */
export function gradeOn(person: Person, date: string): Grade | undefined {
  let found: GradeFrom | undefined;
  for (const g of person.grades) {
    if (g.from !== null && g.from > date) continue;
    if (found === undefined || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

/** Whether the person has any hold. */
export const isHeld = (p: Person) => p.holds.length > 0;

/** Whether a date (yyyy-MM-dd) is within one of the person's holds. */
export const heldOn = (p: Person, date: string) =>
  p.holds.some((h) => date >= h.from && (h.to === null || date <= h.to));

export const isRetired = (p: Person) => p.retiredFrom !== null;

/** Whether a claim of the person's dated on the day (yyyy-MM-dd), open and not bound by a statement, is kept out of
 *  every live total: it is dated within one of their holds, or their retirement date is reached. */
export const setAsideOn = (p: Person, date: string) => p.retiredNow || heldOn(p, date);

export const peopleKey = (sheet: string) => ['people', sheet] as const;

/** The mutation key every action on a person's row shares, so the row shows only its latest action's refusal. */
export const personActionKey = (sheet: string, person: number) => ['person-action', sheet, person] as const;

export const peoplePath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;

/** Everyone on the sheet, in list order (by position; new people go to the end). */
export function usePeople(sheet: string) {
  const read = useReadScope(peopleKey(sheet));
  return useQuery({
    queryKey: read.key,
    queryFn: () => read.get<Person[]>(peoplePath(sheet)),
  });
}

/** Whether retired people are listed: off until turned on, and kept in the URL so loading a sheet leaves it be. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
