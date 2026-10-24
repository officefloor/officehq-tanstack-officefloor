import { useQuery } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
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
// `origin`: the sheet they were transferred from (null: they were not transferred here).
// `retirements`: their past retirements, each ended by an unretire, earliest from-date first: from a date to the
// day they were unretired; a claim dated after the from and before the to is refused, as one after a retirement is.
// `tags`: their tags, trimmed, lower-cased and sorted, without repeats (empty: none); the tag totals count each
// claim of theirs for these as well as for the claim's own.
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
  origin: string | null;
  retirements: Retirement[];
  tags: string[];
};

/** A past retirement: from a date to the day they were unretired (YYYY-MM-DD). */
export type Retirement = { from: string; to: string };

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

/** The sheet's people, in list order: by their place, which the move buttons swap; a new person goes to the end. */
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

const asIds = (raw: unknown): number[] =>
  raw === undefined || raw === null || raw === ''
    ? []
    : String(raw).split(',').map(Number).filter((n) => Number.isInteger(n));

/** The ids of the selected people (search param `selectedPeople`). */
export function useSelectedPeople() {
  const [ids] = useSearchParam('selectedPeople', asIds);
  const navigate = useNavigate();
  const update = (change: (ids: Set<number>) => void) =>
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = { ...prev };
        const set = new Set(asIds(prev.selectedPeople));
        change(set);
        if (set.size === 0) delete next.selectedPeople;
        else next.selectedPeople = [...set].join(',');
        return next;
      },
      replace: true,
    });
  return {
    selected: new Set(ids),
    toggle: (id: number, on: boolean) => update((set) => (on ? set.add(id) : set.delete(id))),
    clear: () => update((set) => set.clear()),
  };
}

export const PEOPLE_SORTS = ['added', 'name', 'grade', 'due-total'] as const;
export type PeopleSort = (typeof PEOPLE_SORTS)[number];

const asSort = (raw: unknown): PeopleSort =>
  (PEOPLE_SORTS as readonly unknown[]).includes(raw) ? (raw as PeopleSort) : 'added';

/** How this screen shows the people list (search param `sortPeople`): added (list order) when first opened, and
 *  left as it is by loading a sheet. It changes only what the screen shows — the list order, which every action
 *  and figure goes by, stays as it is. */
export const useSortPeople = () => useSearchParam('sortPeople', asSort);

/** A name as names are compared: case and surrounding spaces ignored. */
export const nameKey = (name: string) => name.trim().toLowerCase();

/** The people in the order the sort shows them: by the key, those with equal keys in list order (the sort is
 *  stable). `key` gives a person's key; null (an n/a due-total) goes last. */
export function sortedBy<K extends string | number>(people: Person[], key: (p: Person) => K | null): Person[] {
  const keyed = people.map((p, i) => ({ p, i, k: key(p) }));
  keyed.sort((a, b) => {
    if (a.k === null || b.k === null) return a.k === b.k ? a.i - b.i : a.k === null ? 1 : -1;
    return a.k < b.k ? -1 : a.k > b.k ? 1 : a.i - b.i;
  });
  return keyed.map((e) => e.p);
}
