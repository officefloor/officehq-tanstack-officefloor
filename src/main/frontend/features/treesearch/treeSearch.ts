import { getJsonNow } from '../../api/http';
import type { Claim } from '../claims/claims';
import { liveHolds, nameWithGrade } from '../people/people';
import type { Person } from '../people/people';
import { heldParts, personDueTotal } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { floorOf, lateOf } from '../access/access';
import type { Settings } from '../access/access';
import type { Sheet } from '../sheet/sheet';
import { cutTotal } from '../pools/pools';
import type { Pool } from '../pools/pools';

/** One person found: their sheet, their name with their grade and their due-total as their sheet's list shows it. */
export type Found = { sheetId: string; name: string; dueTotal: string };

const enc = encodeURIComponent;

/** Whether the person's name or one of their aliases holds the text, case ignored. */
function matches(p: Person, text: string): boolean {
  const t = text.toLowerCase();
  return [p.name, ...p.aliases].some((n) => n.toLowerCase().includes(t));
}

/** One person of the tree: their sheet, the person and their due-total as their sheet's list shows it. */
export type TreePerson = { sheetId: string; person: Person; dueTotal: string };

/** The people on one sheet, in list order, each read fresh from the store. */
async function sheetPeople(sheetId: string): Promise<TreePerson[]> {
  const [sheet, people, claims, rates, settings, pools] = await Promise.all([
    getJsonNow<Sheet>(`/api/sheets/${enc(sheetId)}`),
    getJsonNow<{ people: Person[] }>(`/api/sheets/${enc(sheetId)}/people`).then((r) => r.people),
    getJsonNow<{ claims: Claim[] }>(`/api/sheets/${enc(sheetId)}/claims`).then((r) => r.claims),
    getJsonNow<{ rates: Rate[] }>(`/api/sheets/${enc(sheetId)}/rates`).then((r) => r.rates),
    getJsonNow<Settings>(`/api/sheets/${enc(sheetId)}/settings`),
    getJsonNow<{ pools: Pool[] }>(`/api/sheets/${enc(sheetId)}/pools`).then((r) => r.pools),
  ]);
  const today = sheet.today;
  const floor = floorOf(settings);
  const late = lateOf(settings, today);
  const cap = settings.cap ?? '';
  return people.map((p) => ({
    sheetId,
    person: p,
    dueTotal: personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutTotal(pools, p.id)),
  }));
}

/**
 * Everyone over the sheet and the other sheets of its tree (its ancestors and descendants) whose
 * name or an alias holds the text, case ignored: the sheet's own people first in list order, then
 * the other sheets' by sheet id.
 */
export async function searchTree(sheetId: string, text: string): Promise<Found[]> {
  const people = await treePeople(sheetId);
  return people
    .filter((t) => matches(t.person, text))
    .map((t) => ({ sheetId: t.sheetId, name: nameWithGrade(t.person), dueTotal: t.dueTotal }));
}

/**
 * Everyone over the sheet and the other sheets of its tree (its ancestors and descendants), read
 * fresh: the sheet's own people first in list order, then the other sheets' by sheet id.
 */
export async function treePeople(sheetId: string): Promise<TreePerson[]> {
  const { sheets } = await getJsonNow<{ sheets: string[] }>(`/api/sheets/${enc(sheetId)}/tree`);
  const found = await Promise.all([sheetId, ...sheets].map(sheetPeople));
  return found.flat();
}
