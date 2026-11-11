import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { liveHolds, peopleKey, usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { NA, heldParts, personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCutTotals } from '../pools/pools';

/** A person in a crew (never its lead). */
export type CrewMember = { personId: string; name: string };

/** A crew on the sheet: its name, its lead (a person on the sheet) and its members in the order added. */
export type Crew = { name: string; leadId: string; lead: string; members: CrewMember[] };

// Under the people's key: a change to a person (a new name) reads the crews again too.
export const crewsKey = (sheetId: string) => [...peopleKey(sheetId), 'crews'] as const;

export const crewsQuery = (sheetId: string) => ({
  queryKey: crewsKey(sheetId),
  queryFn: async () =>
    (await getJson<{ crews: Crew[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`)).crews,
});

/** The crews on the sheet, by name. */
export function useCrews(sheetId: string) {
  return useQuery(crewsQuery(sheetId));
}

/** Each person's due-total by id, as their row shows it. */
export function useDueTotals(sheetId: string): Map<string, string> {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  return new Map(
    people.map((p) => [
      p.id,
      personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id)),
    ]),
  );
}

/** A crew's due-total: its lead's and its members' due-totals added up, n/a skipped. */
export function crewDueTotal(crew: Crew, dues: Map<string, string>): string {
  const ids = [crew.leadId, ...crew.members.map((m) => m.personId)];
  return sumDue(ids.map((id) => dues.get(id) ?? NA));
}
