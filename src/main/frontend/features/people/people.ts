import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';

/** A grade: A, B or C, or one added on the sheet (one to three capital letters). */
export type Grade = string;
/** The grades every sheet has; the sheet's own list is useGradeCodes. */
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** A grade in force from a date (yyyy-MM-dd; '' from the beginning of time) until the next one. */
export type GradeFrom = { from: string; grade: Grade };

/** The grade in force on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it. */
export function gradeOn(grades: GradeFrom[], date: string): Grade {
  let best = grades[0];
  for (const g of grades) if (g.from <= date && g.from >= best.from) best = g;
  return best.grade;
}

/** How the screen shows a person: their name and, in brackets, their grade in force on the sheet's today. */
export const nameWithGrade = (p: Pick<Person, 'name' | 'grade'>) => `${p.name} (${p.grade})`;

// `version` is raised by each save; an edit sends the version it was made from. `statementCount`
// is how many statements have been taken for them; `statedTotal` and `statementDate` are the last
// one's due-total and date ('' before the first). `retired` is set once they have a retirement
// date, `retiredFrom` ('' when not retired), whatever that date is. `held` is whether they have any hold; `holds` their holds in the order added (see `Hold`).
// `patronIds` are the ids of the people they name as their patrons, in the order named, and
// `patronShares` their shares (two decimals, adding up to 100.00), in the same order. `grade` is the grade in force on
// the sheet's today; `grades` every grade they have been given, by from-date, the first from ''.
// `terms` are their own terms in days in force on the sheet's today, ahead of their grade's and the sheet's ('' for none).
// `contact` is their contact, trimmed and lower-cased ('' for none); no two people on a sheet share one.
// `tags` are their tags: trimmed, lower-cased, without repeats, sorted; empty when they have none.
// `aliases` are their aliases, trimmed, in the order given; like names (ignoring case), none equals
// any name or alias on the sheet, nor on another sheet of its tree (its ancestors and descendants).
// `clashing` is whether, all the same, their name or an alias is one on another sheet of the tree
// (they clashed before the rule): every edit of them is refused until it renames them out of it.
// `origin` is the sheet they were transferred from ('' when they never were). `retirements` are the
// retirements they were unretired from, oldest first, each as '<from> to <the day unretired>'.
// `snapshots` are their snapshot lines, one per SNAPSHOT written for them when the sheet's today
// changed, oldest first, each as '<the day being left> <due-total>'. `statementLines` are their
// statements still standing (a cancelled one is gone), oldest first by date, each as
// '<statement date> <due-total>' — those a merge or a transfer brought over among them.
// `flag` is the reason a steward flagged them with while the flag is in force on the sheet's today
// ('' otherwise): while it is, only a steward may open a claim of theirs. `approver` is the user who
// may approve or reject their PENDING claims as a steward may ('' for none).
export type Person = {
  id: string;
  name: string;
  grade: Grade;
  version: number;
  statementCount: number;
  statedTotal: string;
  statementDate: string;
  retired: boolean;
  retiredFrom: string;
  held: boolean;
  holds: Hold[];
  patronIds: string[];
  patronShares: string[];
  grades: GradeFrom[];
  terms: string;
  contact: string;
  tags: string[];
  aliases: string[];
  clashing: boolean;
  origin: string;
  retirements: string[];
  snapshots: string[];
  statementLines: string[];
  flag: string;
  approver: string;
};

/**
 * A hold on a person: from a date to a date (yyyy-MM-dd; `to` '' when open-ended), both inclusive.
 * An OPEN or PENDING claim of theirs dated within one, unless a statement binds it, counts in no total.
 */
export type Hold = { from: string; to: string };

/** Whether the date (yyyy-MM-dd) is within one of the holds. */
export const heldOn = (holds: Hold[], date: string) =>
  holds.some((h) => h.from <= date && (h.to === '' || date <= h.to));

/**
 * The holds a person's claims count against on the sheet's `today`: none of their own holds (a hold
 * only takes away their pool cut), only from the day their retirement date is reached (`today` on or
 * after it) one covering every date — their OPEN and PENDING claims, unless a statement binds them,
 * then count in no total.
 */
export const liveHolds = (p: Pick<Person, 'holds' | 'retiredFrom'>, today: string): Hold[] =>
  p.retiredFrom !== '' && today !== '' && p.retiredFrom <= today ? [{ from: '', to: '' }] : [];

/** The holds a person's claimed counts against on the sheet's `today`: as `liveHolds`. */
export const retiredHolds = (p: Pick<Person, 'holds' | 'retiredFrom'>, today: string): Hold[] =>
  liveHolds(p, today);

export const peopleKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export const peopleQuery = (sheetId: string) => ({
  queryKey: peopleKey(sheetId),
  queryFn: async () =>
    (await getJson<{ people: Person[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people`)).people,
});

/** The sheet's people, in list order: by their position, which the up and down buttons swap. */
export function usePeople(sheetId: string) {
  return useQuery(peopleQuery(sheetId));
}

/** Whether retired people are listed. Off when the screen is first opened; loading a sheet keeps it. */
export const useShowRetired = () => useSearchParam('retired', asFlag);
