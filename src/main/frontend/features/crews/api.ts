import { getJson, postJson } from '../../api/http';
import { sheetKey } from '../sheet/api';
import { sumDues } from '../rates/api';

/** A member of a crew: a person of the sheet. */
export type CrewMember = { personId: number; name: string };

/** A crew on the sheet: its lead (a person of the sheet; null, named '', once they left the sheet) and its
 *  members in the order added. */
export type Crew = { id: number; sheetId: string; name: string; leadId: number | null; lead: string; members: CrewMember[] };

const base = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/crews`;

export const crewsKey = (sheetId: string) => [...sheetKey(sheetId), 'crews'] as const;

/** The sheet's crews, in the order they were added. */
export function listCrews(sheetId: string): Promise<Crew[]> {
  return getJson<Crew[]>(base(sheetId));
}

/** Add a crew (name trimmed) led by a person; refused (rejects) for a blank name, one already used on the
 *  sheet, or no lead. */
export function addCrew(sheetId: string, by: string, name: string, leadId: number | null): Promise<Crew[]> {
  return postJson<Crew[]>(`${base(sheetId)}?by=${encodeURIComponent(by)}`, { name, leadId });
}

/** Add a person to a crew; refused (rejects) for the crew's lead or a person already in the crew. */
export function addCrewMember(sheetId: string, by: string, crewId: number, personId: number | null): Promise<Crew[]> {
  return postJson<Crew[]>(`${base(sheetId)}/${crewId}/members?by=${encodeURIComponent(by)}`, { personId });
}

/** A crew's due-total: its lead's and its members' due-totals (as their rows show them; null: n/a)
 *  added up, n/a ones skipped. */
export function crewDueTotal(crew: Crew, dueOf: ReadonlyMap<number, string | null>): string {
  return sumDues([crew.leadId, ...crew.members.map((m) => m.personId)].map((id) => (id === null ? null : dueOf.get(id) ?? null)));
}
