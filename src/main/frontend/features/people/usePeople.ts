import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** `version` counts the person's saves; an edit sends the version it loaded, so a save made by
 *  someone else in the meantime refuses it. `statementCount` is how many statements have been
 *  taken for them; `statedTotal` the last one's due-total (two decimals, null when it was n/a or
 *  none was taken) and `statementDate` its date (yyyy-mm-dd, null when none was taken).
 *  `retired` is whether a retirement date is set (whatever the date), `retiredFrom` that date
 *  (yyyy-mm-dd, null when not retired). `pastRetirements` are the retirements ended by unretiring
 *  them, in the order they ended, from the retirement date to the day unretired; no claim dated after
 *  the from and before the to of one is accepted. `held` is whether they have any hold; `holds` every hold, in the
 *  order added, from a date to a date (both inclusive; `to` null: open-ended). Their OPEN claims
 *  dated within a hold count in no live total, nor any OPEN or PENDING claim of theirs once the sheet's today
 *  reaches their retirement date, unless a statement binds them. `patrons` are the ids of the people they have named as patrons, in
 *  the order they were named. `grade` is the grade in force on the sheet's today when they were read;
 *  `grades` every grade recorded, by from-date (null: from the beginning of time, first). `terms` are
 *  the person's own terms in days in force on the sheet's today (null: none), ahead of their grade's
 *  and the sheet's; `ownTerms` their own terms from the beginning of time (null: none), and
 *  `datedTerms` every dated terms recorded, by from-date (`terms` null: none from then).
 *  `contact` is the person's contact, trimmed and lower-cased (null: none). `aliases` are the
 *  person's aliases, sorted as names are compared; the quick-add bar and the paste box find a
 *  person by an alias as well as by the name. `clashing` is whether their name or an alias is used
 *  on another sheet of the sheet's tree (its ancestors and descendants); such a person's edits are
 *  refused until one renames them to a name that does not clash. `origin` is the sheet the person
 *  was transferred from (null: they were not transferred here). `tags` are the person's tags,
 *  lower-cased and sorted (empty: none); a claim counts in a tag's total when it or its person carries it. */
export type Person = {
  id: number;
  name: string;
  grade: string;
  grades: { grade: string; from: string | null }[];
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  pastRetirements: { from: string; to: string }[];
  held: boolean;
  holds: { from: string; to: string | null }[];
  patrons: number[];
  terms: number | null;
  ownTerms: number | null;
  datedTerms: { terms: number | null; from: string }[];
  contact: string | null;
  aliases: string[];
  clashing: boolean;
  origin: string | null;
  tags: string[];
  /** Snapshot lines, oldest first: `<day left> <due-total>` (n/a when it was n/a). */
  snapshots: string[];
  /** Statements still standing, oldest first: `<statement date> <due-total>` (n/a when it was n/a). */
  statementLines: string[];
  /** The reason of the person's flag while it is in force (the sheet's today on or before its
   *  until-date), null when none is; only a steward may open their claims then. */
  flag: string | null;
  /** The user who may approve or reject the person's PENDING claims as a steward may, null when none. */
  approver: string | null;
};

/** Whether the sheet's `today` (yyyy-mm-dd) has reached the person's retirement date. */
export function retiredOut(person: Person, today: string): boolean {
  return person.retiredFrom !== null && today !== '' && today >= person.retiredFrom;
}

/** Whether a claim of the person's dated `date` (yyyy-mm-dd) is held out of the live totals: the sheet's
 *  `today` has reached their retirement date. A hold does not take a claim out (only a pool cut). */
export function heldOn(person: Person, _date: string, today: string): boolean {
  return retiredOut(person, today);
}

/** By person id, whether a claim dated then is held out (see `heldOn`) on the sheet's `today`. */
export function heldOnBy(people: Person[], today: string): (personId: number, date: string) => boolean {
  const byId = new Map(people.map((p) => [p.id, p]));
  return (personId, date) => {
    const p = byId.get(personId);
    return p !== undefined && heldOn(p, date, today);
  };
}

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people in list order (by position; see the move buttons), re-read whenever the sheet is loaded. */
export function usePeople(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(peopleKey(sheet), asOf),
    queryFn: () => getJson<Person[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/people`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}

/** The person's grade in force on `date` (yyyy-mm-dd): the one from the latest date on or before
 *  it, the first one they were given applying from the beginning of time. */
export function gradeOn(person: Person, date: string): string {
  let found = person.grades[0]?.grade ?? person.grade;
  for (const g of person.grades) if (g.from === null || g.from <= date) found = g.grade;
  return found;
}

/** How the screen shows a person's name: `<name> (<grade in force on date>)`. */
export function shownName(person: Person, date: string): string {
  return `${person.name} (${gradeOn(person, date)})`;
}
