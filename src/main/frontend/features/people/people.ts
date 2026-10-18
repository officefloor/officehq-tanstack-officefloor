import { useQuery } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

export type Person = { id: number; name: string;
  /** The grade in force on the sheet's today (null before all their grades). */
  grade: string | null;
  /** Every grade recorded, by from-date (yyyy-mm-dd; null: from the beginning of time). */
  grades: { from: string | null; grade: string }[];
  version: number;
  /** The date retired from (yyyy-mm-dd); null while not retired. A claim dated after it is refused. */
  retiredFrom: string | null;
  /** Whether they have any hold. */
  held: boolean;
  /** Their holds, by from-date (yyyy-mm-dd, both inclusive; from null: the beginning of time; to null:
   *  open-ended). Their OPEN and PENDING claims dated within one count in no live total. */
  holds: { from: string | null; to: string | null }[];
  /** The ids of the other people on the sheet named as their patrons. */
  patronIds: number[] };
/** An edit carries the version of the person it was loaded from; the server refuses it (409) when
 *  someone else has saved the person since. */
export type PersonInput = { name: string; grade: string; version?: number;
  /** The date the grade applies from (empty: the sheet's today); a person's first grade applies from
   *  the beginning of time. */
  gradeFrom?: string };

/** The person's grade in force on a date (yyyy-mm-dd): the latest from-date on or before it. */
export function gradeOn(person: Person | undefined, date: string | undefined): string | undefined {
  if (person === undefined || date === undefined) return undefined;
  let found: { from: string | null; grade: string } | undefined;
  for (const g of person.grades) {
    if (g.from !== null && g.from > date) continue;
    if (found === undefined || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade;
}

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/people`;
export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The active sheet's people, in the order they were added, retired or not (every total counts them). */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleKey(sheet ?? ''),
    queryFn: () => getJson<{ people: Person[] }>(base(sheet!)).then((r) => r.people),
    enabled: !!sheet,
  });
}

export const addPerson = (sheet: string, user: string, input: PersonInput) =>
  postJson<Person>(base(sheet), input, user);

export const updatePerson = (sheet: string, user: string, id: number, input: PersonInput) =>
  putJson<Person>(`${base(sheet)}/${id}`, input, user);

/** The outcome of an import: the counts added and updated, or the numbers of the lines refused
 *  (nothing imported). */
export type ImportResult = { added: number; updated: number; errors: number[] };

/** Imports people pasted as lines of name,grade; a refusal (400) carries the bad line numbers. */
/** Retires a person from a date (empty: the sheet's today); refused (400) when already retired. */
export const retirePerson = (sheet: string, user: string, id: number, from: string) =>
  postJson<Person>(`${base(sheet)}/${id}/retire`, { from }, user);

/** Adds a hold on a person from a date (empty: the sheet's today) to a date (empty: open-ended);
 *  refused (400) when the to-date is before the from-date. */
export const holdPerson = (sheet: string, user: string, id: number, from: string, to: string) =>
  postJson<Person>(`${base(sheet)}/${id}/hold`, { from, to }, user);

/** Whether a date (yyyy-mm-dd) falls within one of the person's holds. */
export const heldOn = (person: Person | undefined, date: string) =>
  !!person?.holds.some((h) => (h.from === null || h.from <= date) && (h.to === null || date <= h.to));

/** Names another person on the sheet as the person's patron; refused (400) for themselves or one
 *  already named. */
export const addPatron = (sheet: string, user: string, id: number, patronId: number) =>
  postJson<Person>(`${base(sheet)}/${id}/patrons`, { patronId }, user);

/** Whether a claim dated `date` would be after the person's retirement (and so refused). */
export const afterRetirement = (person: Person | undefined, date: string) =>
  !!person?.retiredFrom && date > person.retiredFrom;

/** Whether retired people are listed too, kept in the URL. */
export const useShowRetired = () => useSearchParam('showRetired', asFlag);

export const importPeople = (sheet: string, user: string, text: string) =>
  postJson<ImportResult>(`${base(sheet)}/import`, { text }, user);

/** The person open in the editor (by id), kept in the URL. */
export const useEditingPerson = () => useSearchParam('editPerson', asString);
