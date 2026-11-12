import { useMutationState, useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** `version` moves on with each stored edit; an edit is saved only over the version its editor loaded.
 *  `statementCount` is how many statements were taken of them; `statedTotal` (two decimals, or n/a)
 *  and `statementDate` (yyyy-MM-dd) are the last one's, null when none was taken. `retired` is set
 *  as soon as a `retiredFrom` date (yyyy-MM-dd, null when not retired) is, whatever the date;
 *  `retiredNow` once the sheet's today reaches it: from then their OPEN claims count in no live total
 *  unless a statement bound them.
 *  `holds` are their holds, oldest first; `held` whether they have any. A hold leaves their claims in
 *  every total: one in force on the sheet's today only takes their cuts from their pools.
 *  `patronIds` are the ids of the other people on the sheet they named as their patrons, in the order they named them.
 *  `grade` is the grade in force on the sheet's today; `grades` every grade with the date it applies from.
 *  `terms` are their own terms in force on the sheet's today, in whole days (null when none), ahead of their grade's and the sheet's.
 *  `contact` is their contact (trimmed, lower-cased; null when none), no other person's on the sheet.
 *  `aliases` are other names they are found by (no name or other alias on the sheet), sorted as names are compared.
 *  `clashing` whether their name or an alias is used as a name or alias on another sheet of the sheet's tree (its
 *  ancestors and descendants); every edit of them is refused until it renames them to a name that does not clash.
 *  `origin` is the id of the sheet they were transferred from (null when they were not).
 *  `pastRetirements` are the retirements they were unretired from, oldest first.
 *  `tags` are their tags (trimmed, lower-cased, no repeats), sorted; a claim counts for a tag when it or its
 *  person carries it.
 *  `snapshots` are their snapshots, one per SNAPSHOT written for them as the sheet's today moved on, oldest first.
 *  `statements` are their statements still standing (a cancelled one is gone), oldest first by date, the ones a
 *  merge or a transfer brought over among them.
 *  `flag` is the reason of their flag while it is in force on the sheet's today (null when none is): only a
 *  steward may open a claim of theirs then.
 *  `approver` is the user who may approve or reject their PENDING claims as a steward may (null when none). */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: Grades;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  retiredNow: boolean;
  held: boolean;
  holds: Hold[];
  patronIds: number[];
  terms: number | null;
  contact: string | null;
  aliases: string[];
  clashing: boolean;
  origin: string | null;
  pastRetirements: PastRetirement[];
  tags: string[];
  snapshots: Snapshot[];
  statements: StatementLine[];
  flag: string | null;
  approver: string | null;
};

/** A statement: its date (yyyy-MM-dd) and the person's due-total it recorded (two decimals, or n/a). */
export type StatementLine = { date: string; dueTotal: string };

/** A snapshot: the day being left (yyyy-MM-dd) and the person's due-total as it stood then (two decimals, or n/a). */
export type Snapshot = { day: string; dueTotal: string };

/** A past retirement: from the date retired from to the day unretired (yyyy-MM-dd). No claim dated
 *  after its from-date and before its to-date may be added. */
export type PastRetirement = { from: string; to: string };

/** A hold from its from-date to its to-date (yyyy-MM-dd, both inclusive; null for no end). */
export type Hold = { from: string; to: string | null };

/** Whether the hold covers the date (yyyy-MM-dd). */
export const covers = (h: Hold, date: string) => h.from <= date && (h.to === null || date <= h.to);

export const GRADES = ['A', 'B', 'C'] as const;

/** A person's name as the screen shows it: <name> (<grade in force on the sheet's today>). */
export const shownName = (p: Person) => `${p.name} (${p.grade})`;

/** A person's grades, each with the date (yyyy-MM-dd) it applies from; the first has none (null),
 *  applying from the beginning of time. */
export type Grades = { grade: string; from: string | null }[];

/** The grade in force on the date (yyyy-MM-dd): the one with the latest from-date on or before it. */
export function gradeOn(grades: Grades, on: string | undefined): string {
  let best = grades[0];
  for (const g of grades) {
    if (g.from === null || (on !== undefined && g.from <= on)) {
      if ((best.from ?? '') <= (g.from ?? '')) best = g;
    }
  }
  return best?.grade ?? 'A';
}

/** Under the sheet's key, so loading the sheet again re-reads its people. */
export const peopleKey = (sheet: string) => [...sheetKey(sheet), 'people'] as const;

export function usePeople(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(peopleKey(sheet), asOf),
    queryFn: () => getJson<Person[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/people`, asOf)),
    enabled: sheet !== '',
  });
}

/** The id of the person whose editor is open in the person form ('' when it adds). */
export const useEditingPerson = () => useSearchParam('editPerson', asString);

/** Whether the people list shows retired people too; off when the screen is first opened. */
export const useShowRetired = () => useSearchParam('showRetired', asFlag);

/** The mutation key of an action on a person's row (a statement, a hold, a retirement): the row
 *  shows only the error of the latest one taken, so taking the next action clears an earlier one's. */
export const personActionKey = (sheet: string, personId: number) => [...peopleKey(sheet), personId, 'action'] as const;

/** When the latest action on the person's row was taken (0 when none was). */
export function useLatestPersonAction(sheet: string, personId: number): number {
  const taken = useMutationState({
    filters: { mutationKey: personActionKey(sheet, personId) },
    select: (m) => m.state.submittedAt,
  });
  return Math.max(0, ...taken);
}

/** The ids of the people selected for a bulk grade, kept in the URL (`selectedPeople`, comma separated). */
export function useSelectedPeople(): [number[], (ids: number[]) => void] {
  const [raw, setRaw] = useSearchParam('selectedPeople', asString);
  const ids = raw.split(',').filter((s) => s !== '').map(Number).filter((n) => !Number.isNaN(n));
  return [ids, (next) => setRaw(next.join(','))];
}
