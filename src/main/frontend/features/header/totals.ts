import { live, useClaims, type ClaimStatus } from '../claims/claims';
import { overdue } from '../overdue/overdue';
import { usePeople } from '../people/people';
import { personDue, sum, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';

// The sheet-wide figures shown in more than one place (the rollups, the summary and the header), worked out once
// here so every place that shows one shows the same value.

/** The statuses claims are counted by. */
export const STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** The sheet's due-total: everyone's due-total added up, skipping n/a. */
export function useSheetDueTotal(sheetId: string): string {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  return sum(
    (people.data ?? []).map((p) =>
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p]), rates.data, p, late, floor, cap),
    ),
  );
}

/** The sheet's overdue count and overdue-total: everyone's added up. */
export function useSheetOverdue(sheetId: string): { count: number; total: string } {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const all = (people.data ?? []).map((p) =>
    overdue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p]), rates.data, p, late, floor),
  );
  return { count: all.reduce((n, o) => n + o.count, 0), total: sum(all.map((o) => o.total)) };
}

/** How many of the sheet's claims have each status. */
export function useStatusCounts(sheetId: string): (status: ClaimStatus) => number {
  const claims = useClaims(sheetId);
  return (status) => (claims.data ?? []).filter((c) => c.status === status).length;
}
