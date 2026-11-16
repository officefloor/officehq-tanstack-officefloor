import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';
import type { Claim } from '../claims/api';

/** A grade's code: A, B, C, or one added to the sheet (one to three capital letters). */
export type Grade = string;
/** The grades every sheet has; a sheet's added grades follow them (see `useGradeCodes`). */
export const GRADES: Grade[] = ['A', 'B', 'C'];

/** A grade a person was given, in force from its from-date (yyyy-MM-dd; null: from the beginning of time). */
export type GradeFrom = { grade: Grade; from: string | null };

/** A hold on a person: from its from-date to its to-date (null: open-ended), both included (yyyy-MM-dd). */
export type Hold = { from: string; to: string | null };

/** `grade` is the one in force on the sheet's today, of their `grades` by from-date (none first).
 *  `version` is raised by every saved edit; an edit sends the version its form loaded. Their
 *  statements: how many were taken, and the last one's due-total and date (null until the first).
 *  `retiredFrom` is the date they are retired from (null: not retired); with it set at all they
 *  are `retired`, whatever the date; `retiredReached` once the sheet's today is on or after it (their
 *  open claims not bound by a statement then count in no live total). `holds` are their holds in the order added, and `held` whether
 *  they have any: their OPEN claims dated within a hold, and not bound by a statement, count in no
 *  live total. `patronIds` are the people they name
 *  as patrons, in the order named. */
/** A past retirement: from its from-date to the day they were unretired (yyyy-MM-dd). */
export type Retirement = { from: string; to: string };

export type Snapshot = { day: string; dueTotal: string };

/** A statement still standing: its date (yyyy-MM-dd) and the due-total it recorded (or n/a). */
export type StatementLine = { date: string; dueTotal: string };

export type Person = {
  id: number; sheetId: string; name: string; grade: Grade; grades: GradeFrom[]; version: number;
  statementCount: number; statedTotal: string | null; statementDate: string | null;
  retired: boolean; retiredFrom: string | null; retiredReached: boolean;
  /** Their past retirements, oldest first: a claim dated after one's from and before its to is refused. */
  retirements: Retirement[];
  /** The SNAPSHOTs written for them, oldest first: the day being left and their due-total on it (or n/a). */
  snapshots: Snapshot[];
  /** Their statements still standing (a cancelled one is gone), oldest first: by date, then as taken. */
  statements: StatementLine[];
  held: boolean; holds: Hold[]; patronIds: number[];
  /** Their flag's reason while it is in force (the sheet's today on or before its until-date; null:
   *  none): only a steward may open their claims then. */
  flagShown: string | null;
  /** The user who may approve or reject their PENDING claims as a steward may (null: none). */
  approver: string | null;
  /** Their own terms in whole days in force on the sheet's today (null: none): a claim's due-by goes by
   *  those in force on its date before their grade's or the sheet's. */
  terms: number | null;
  /** Their own terms from the beginning of time (null: none). */
  baseTerms: number | null;
  /** Their own terms given from a from-date, by from-date (null terms: none from it). */
  datedTerms: TermsFrom[];
  /** Their contact, trimmed and lower-cased (null: none); no two people on a sheet share one. */
  contact: string | null;
  /** Their tags, sorted and comma separated (null: none); a claim counts for the tag totals under
   *  each of its own tags and its person's. */
  tags: string | null;
  /** The sheet they were last transferred from (null: none). */
  origin: string | null;
  arrivedOn: string | null;
  /** Their aliases, sorted as names are compared; the quick-add bar and the paste box find them by one. */
  aliases: string[];
  /** Whether their name or an alias is used on another sheet of the sheet's tree (its ancestors and
   *  descendants): until renamed clear of it, every edit through the person form is refused. */
  clashing: boolean;
};
export type TermsFrom = { terms: number | null; from: string };

/** `gradeFrom` is the date an edit's grade applies from (blank: the sheet's today); `terms` their
 *  own terms in whole days (blank: none), from `termsFrom` (blank: the beginning of time, the
 *  dated ones kept); `contact` their contact (blank: none); `tags` their tags,
 *  comma separated (blank: none). */
export type PersonInput = { name: string; grade: string; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string };

/** The person's grade in force on a date (yyyy-MM-dd): the one with the latest from-date on or before it. */
export function gradeOn(p: Person, date: string): string {
  let found: GradeFrom | undefined;
  for (const g of p.grades) {
    if (g.from !== null && g.from > date) continue;
    if (found === undefined || (found.from ?? '') < (g.from ?? '')) found = g;
  }
  return found?.grade ?? '';
}

/** A person's name as the screen shows it: `<name> (<grade in force on the sheet's today>)`. */
export function shownName(p: Person): string {
  return `${p.name} (${p.grade})`;
}

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/people`;

export const peopleKey = (sheetId: string) => [...sheetKey(sheetId), 'people'] as const;

/** The sheet's people, in list order (each one's place in the list; new people go to the end). */
export function listPeople(sheetId: string, live = false): Promise<Person[]> {
  return getJson<Person[]>(base(sheetId), live);
}

/** Add a person; refused (rejects) when the name is empty or already in use on the sheet. */
export function addPerson(sheetId: string, by: string, input: PersonInput): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, input);
}

/** How many people an import added, and how many it updated. */
export type ImportResult = { added: number; updated: number };

/** Add a person per line of name,grade,terms,contact (terms and contact may be absent or blank), in
 *  order, each as the person form adds them, or update the person whose name or alias the line's name
 *  matches as the person form edits them (grade from the sheet's today; terms and contact when given);
 *  refused (rejects), importing nothing, when any line would be refused by the person form or repeats a
 *  name, contact or person of another line. */
export function importPeople(sheetId: string, by: string, text: string): Promise<ImportResult> {
  return postJson<ImportResult>(`/api/sheets/${encodeURIComponent(sheetId)}/import-people?by=${encodeURIComponent(by)}`, { text });
}

/** Edit a person loaded at `version`; refused (rejects) like an add, and with a 409 when someone
 *  else has saved the person since. */
export function updatePerson(sheetId: string, by: string, id: number, version: number, input: PersonInput): Promise<Person> {
  return putJson<Person>(`${base(sheetId)}/${id}?by=${encodeURIComponent(by)}`, { ...input, version });
}

/** Take a statement of the person: records their due-total now and binds each of their counting
 *  claims to the figure it shows now (unbinding the others). Taken as of a date (`asof`; blank: now),
 *  it goes by everything as it stood at the end of that date, which stands in for the sheet's today;
 *  refused (rejects) for a date after the sheet's today. */
export function takeStatement(sheetId: string, by: string, id: number, asof = ''): Promise<Person> {
  const on = asof === '' ? '' : `&asof=${encodeURIComponent(asof)}`;
  return postJson<Person>(`${base(sheetId)}/${id}/statement?by=${encodeURIComponent(by)}${on}`, {});
}

/** A steward cancels the person's last statement: the claims it bound become live again and the
 *  person shows the statement before it (or none); refused (rejects) for anyone else, and for a
 *  person with no statement. */
export function cancelStatement(sheetId: string, by: string, id: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/statement-cancel?by=${encodeURIComponent(by)}`, {});
}

/** Retire the person from `from` (blank: the sheet's today); refused (rejects) when it is not a date. */
export function retirePerson(sheetId: string, by: string, id: number, from: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/retire?by=${encodeURIComponent(by)}`, { from });
}

/** Unretire the person on the sheet's today, keeping the retirement as a past one; refused
 *  (rejects) for anyone not retired. */
export function unretirePerson(sheetId: string, by: string, id: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/unretire?by=${encodeURIComponent(by)}`, {});
}

/** Swap the person with the one above (`up`) or below (`down`) them in the list; refused (rejects)
 *  for the first moving up or the last moving down. */
export function movePerson(sheetId: string, by: string, id: number, direction: 'up' | 'down'): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/move?by=${encodeURIComponent(by)}`, { direction });
}

/** Add a hold on the person from `from` (blank: the sheet's today) to `to` (blank: open-ended);
 *  refused (rejects) when a date is not a date or `to` is before `from`. */
export function holdPerson(sheetId: string, by: string, id: number, from: string, to: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/hold?by=${encodeURIComponent(by)}`, { from, to });
}

/** A steward flags the person with `reason` until `until` (blank: the sheet's today), replacing any
 *  flag before; refused (rejects) for anyone else, a blank reason, or an until that is not a date. */
export function flagPerson(sheetId: string, by: string, id: number, reason: string, until: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/flag?by=${encodeURIComponent(by)}`, { reason, until });
}

/** Name the user (blank: none) who may approve or reject the person's PENDING claims as a steward
 *  may, replacing any before. */
export function setApprover(sheetId: string, by: string, id: number, user: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/approver?by=${encodeURIComponent(by)}`, { user });
}

/** Name another person on the sheet as one of the person's patrons; refused (rejects) for the
 *  person themselves or a patron already named. */
export function addPatron(sheetId: string, by: string, id: number, patronId: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/patrons?by=${encodeURIComponent(by)}`, { patronId });
}

/** Give the person an alias; refused (rejects) when it is blank or equals, as names are compared,
 *  any name or alias on the sheet. */
export function addAlias(sheetId: string, by: string, id: number, alias: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/aliases?by=${encodeURIComponent(by)}`, { alias });
}

/** A steward merges the person into another on the sheet (`intoId`): their claims, aliases, patrons
 *  and pool memberships go to the other and they leave the sheet; refused (rejects) for anyone else,
 *  for the person themselves, and when either of them is retired. */
export function mergePerson(sheetId: string, by: string, id: number, intoId: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/merge?by=${encodeURIComponent(by)}`, { intoId });
}

/** A steward transfers the person to another sheet by id (`to`): they arrive there with their grades,
 *  terms, contact, aliases and claims, and leave this sheet entirely; someone there with their name or an
 *  alias has them merged into them, as a merge does. Refused (rejects) for anyone else, a retired person,
 *  this sheet's own id, names matching more than one person there or a retired one, and (when not
 *  merged) a contact already on the other sheet. */
export function transferPerson(sheetId: string, by: string, id: number, to: string): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/transfer?by=${encodeURIComponent(by)}`, { to });
}

/** A steward undoes the transfer that brought the person to this sheet, on the sheet's today they
 *  arrived: they are transferred back to the sheet they came from, as a transfer does. Refused
 *  (rejects) for anyone else, a person who did not arrive by transfer, and any later day. */
export function undoTransfer(sheetId: string, by: string, id: number): Promise<Person> {
  return postJson<Person>(`${base(sheetId)}/${id}/transfer-undo?by=${encodeURIComponent(by)}`, {});
}

export type QuickAddInput = { name: string; amount: string };

/** Add a DRAFT claim of `amount` (the sheet's today, empty note) to the named person, adding them
 *  first with grade A when the name is not on the sheet; refused (rejects), adding no one, when the
 *  name is empty or the claim would be refused. */
export function quickAdd(sheetId: string, by: string, input: QuickAddInput): Promise<Claim> {
  return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick-add?by=${encodeURIComponent(by)}`, input);
}

export const watchesKey = (sheetId: string, user: string) => [...sheetKey(sheetId), 'watches', user] as const;

/** The ids of the people the user watches on the sheet (each user watches for themselves). */
export function listWatches(sheetId: string, user: string): Promise<number[]> {
  return getJson<number[]>(`/api/sheets/${encodeURIComponent(sheetId)}/watches?by=${encodeURIComponent(user)}`);
}

/** Start (`on`) or stop the user watching the person: a watched person's claim settled, approved or
 *  re-settled notifies each watcher. */
export function watchPerson(sheetId: string, by: string, id: number, on: boolean): Promise<{ personId: number; watched: boolean }> {
  return postJson(`${base(sheetId)}/${id}/watch?by=${encodeURIComponent(by)}`, { on });
}

/** How many of the selected people a bulk grade gave the grade. */
export type BulkGradeResult = { graded: number };

/** Give the selected people `grade` together from `from` (blank: the sheet's today), in list order,
 *  each as the person form would; refused (rejects) with no one selected or a from-date not a date. */
export function bulkGradePeople(sheetId: string, by: string, personIds: number[], grade: string, from: string): Promise<BulkGradeResult> {
  return postJson<BulkGradeResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-grade?by=${encodeURIComponent(by)}`, { personIds, grade, from });
}

/** How many of the selected people a bulk retire retired. */
export type BulkRetireResult = { retired: number };

/** Retire the selected people together from `from` (blank: the sheet's today), in list order, each
 *  as the row's retire; refused (rejects) with no one selected or a date not a date. */
export function bulkRetirePeople(sheetId: string, by: string, personIds: number[], from: string): Promise<BulkRetireResult> {
  return postJson<BulkRetireResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-retire?by=${encodeURIComponent(by)}`, { personIds, from });
}
