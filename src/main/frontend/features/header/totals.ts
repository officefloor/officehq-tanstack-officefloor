import { live, useClaims, type ClaimStatus } from '../claims/claims';
import { overdue } from '../overdue/overdue';
import { usePeople } from '../people/people';
import { personDue, personSplit, sum, trimmedSum, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';

// The sheet-wide figures shown in more than one place (the rollups, the summary and the header), worked out once
// here so every place that shows one shows the same value.

/** The statuses claims are counted by. */
export const STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/** The sheet's due-total: everyone's due-total added up, skipping n/a, leaving out the single highest and lowest
 *  when three or more people have one. */
export function useSheetDueTotal(sheetId: string): string {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  return trimmedSum(
    (people.data ?? []).map((p) =>
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, cap),
    ),
  );
}

/** The sheet's classic due-total: everyone's due-total added up, skipping n/a, with no trimming. */
export function useSheetDueSum(sheetId: string): string {
  const due = usePeopleDue(sheetId);
  return sum([...due.values()]);
}

/** Each person's due-total as their row shows it (two decimals, or n/a), by person id. */
export function usePeopleDue(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  return new Map(
    (people.data ?? []).map((p) => [
      p.id,
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, cap),
    ]),
  );
}

/** The sheet's settled-total and open-total: everyone's added up (not trimmed). */
export function useSheetSplit(sheetId: string): { settled: string; open: string } {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const all = (people.data ?? []).map((p) =>
    personSplit(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor),
  );
  return { settled: sum(all.map((s) => s.settled)), open: sum(all.map((s) => s.open)) };
}

/** The sheet's overdue count and overdue-total: everyone's added up (the total leaving out the single highest and
 *  lowest when there are three or more people). */
export function useSheetOverdue(sheetId: string): { count: number; total: string } {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const all = (people.data ?? []).map((p) =>
    overdue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor),
  );
  return { count: all.reduce((n, o) => n + o.count, 0), total: trimmedSum(all.map((o) => o.total)) };
}

/** The signed-in user's own due-total and overdue-total: those of the person on the sheet whose name or an alias is the
 *  user's name (as names are compared: case ignored), or null when there is no such person. */
export function useMyFigures(sheetId: string, user: string): { due: string; overdue: string } | null {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const key = user.trim().toLowerCase();
  const p = (people.data ?? []).find((x) => [x.name, ...(x.aliases ?? [])].some((n) => n.trim().toLowerCase() === key));
  if (!p || key === '') return null;
  const mine = live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today);
  return {
    due: personDue(mine, rates.data, p, late, floor, cap),
    overdue: overdue(mine, rates.data, p, late, floor).total,
  };
}

/** How many of the sheet's claims have each status. */
export function useStatusCounts(sheetId: string): (status: ClaimStatus) => number {
  const claims = useClaims(sheetId);
  return (status) => (claims.data ?? []).filter((c) => c.status === status).length;
}
