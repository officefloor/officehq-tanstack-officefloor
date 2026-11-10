import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimsQuery } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { liveHolds, peopleQuery } from '../people/people';
import type { Person } from '../people/people';
import { personDueTotal, trimmedSumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';
import { personOverdue } from '../overdue/overdue';
import { floorOf, lateOf } from '../access/access';
import type { Settings } from '../access/access';
import type { Sheet } from '../sheet/sheet';

/** A sheet's own figures, as its header shows them: its people, its due-total and its overdue-total. */
export type Figures = { people: number; dueTotal: string; overdueTotal: string };

/** A sheet's figures from what it holds — the one working the header and the consolidated figures share. */
export function sheetFigures(
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  today: string,
  floor: Dated,
  late: Late,
  cap: string,
): Figures {
  const claimsOf = (id: string) => claims.filter((c) => c.personId === id);
  return {
    people: people.length,
    dueTotal: trimmedSumDue(
      people.map((p) => personDueTotal(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late, cap)),
    ),
    overdueTotal: trimmedSumDue(
      people.map((p) => personOverdue(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late).total),
    ),
  };
}

const enc = encodeURIComponent;

/** A sheet's figures, every part of it read fresh from the store. */
async function readFigures(sheetId: string): Promise<Figures> {
  const [sheet, people, claims, rates, settings] = await Promise.all([
    getJson<Sheet>(`/api/sheets/${enc(sheetId)}`),
    peopleQuery(sheetId).queryFn(),
    claimsQuery(sheetId).queryFn(),
    getJson<{ rates: Rate[] }>(`/api/sheets/${enc(sheetId)}/rates`).then((r) => r.rates),
    getJson<Settings>(`/api/sheets/${enc(sheetId)}/settings`),
  ]);
  return sheetFigures(people, claims, rates, sheet.today, floorOf(settings), lateOf(settings, sheet.today), settings.cap ?? '');
}

/**
 * The figures of each of the sheet's descendants: the sheets naming it as their parent, theirs, and
 * so on. Kept under the sheet's own key, so loading the sheet again re-reads them all.
 */
export function useDescendantFigures(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants'] as const,
    queryFn: async () => {
      const { descendants } = await getJson<{ descendants: string[] }>(`/api/sheets/${enc(sheetId)}/descendants`);
      return Promise.all(descendants.map(readFigures));
    },
  });
}
