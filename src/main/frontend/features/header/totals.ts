import { live, useClaims, type ClaimStatus } from '../claims/claims';
import { crewDueTotal, useCrews } from '../crews/crews';
import { overdue } from '../overdue/overdue';
import { gradeOn, usePeople, type Person } from '../people/people';
import { personDue, personSplit, shareParts, sum, trimmedSum, useRates, type SharePart } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { personCutTotal, usePools } from '../pools/pools';
import { useWatches } from '../watch/watch';

// The sheet-wide figures shown in more than one place (the rollups, the summary and the header), worked out once
// here so every place that shows one shows the same value.

/** The statuses claims are counted by. */
export const STATUSES: readonly ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/** Each person's share parts of the sheet's shared claims that count live (see shareParts), by person id. */
export function useShareParts(sheetId: string): (personId: number) => SharePart[] {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const parts = shareParts(live(claims.data ?? [], people.data, late.today), people.data, rates.data, late, floor);
  return (personId) => parts.get(personId) ?? [];
}

/** The sheet's due-total: everyone's due-total added up, skipping n/a, leaving out the single highest and lowest
 *  when three or more people have one. */
export function useSheetDueTotal(sheetId: string): string {
  const parts = useShareParts(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const pools = usePools(sheetId);
  return trimmedSum(
    (people.data ?? []).map((p) =>
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, cap, personCutTotal(pools.data, p.id), parts(p.id)),
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
  const parts = useShareParts(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const pools = usePools(sheetId);
  return new Map(
    (people.data ?? []).map((p) => [
      p.id,
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, cap, personCutTotal(pools.data, p.id), parts(p.id)),
    ]),
  );
}

/** The sheet's settled-total and open-total: everyone's added up (not trimmed). */
export function useSheetSplit(sheetId: string): { settled: string; open: string } {
  const parts = useShareParts(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const all = (people.data ?? []).map((p) =>
    personSplit(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor, parts(p.id)),
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

/** Each person's overdue-total (two decimals), by person id. */
export function usePeopleOverdue(sheetId: string): Map<number, string> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return new Map(
    (people.data ?? []).map((p) => [
      p.id,
      overdue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today), rates.data, p, late, floor).total,
    ]),
  );
}

/** The person on the sheet whose name or an alias is the user's name (as names are compared: case ignored), or
 *  undefined when there is no such person. */
function findUser(people: Person[], user: string): Person | undefined {
  const key = user.trim().toLowerCase();
  if (key === '') return undefined;
  return people.find((x) => [x.name, ...(x.aliases ?? [])].some((n) => n.trim().toLowerCase() === key));
}

/** The signed-in user's own due-total and overdue-total: those of the person on the sheet whose name or an alias is the
 *  user's name (as names are compared: case ignored), or null when there is no such person. */
export function useMyFigures(sheetId: string, user: string): { due: string; overdue: string } | null {
  const parts = useShareParts(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const cap = useCap(sheetId);
  const pools = usePools(sheetId);
  const p = findUser(people.data ?? [], user);
  if (!p) return null;
  const mine = live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p], late.today);
  return {
    due: personDue(mine, rates.data, p, late, floor, cap, personCutTotal(pools.data, p.id), parts(p.id)),
    overdue: overdue(mine, rates.data, p, late, floor).total,
  };
}

/** The signed-in user's crew total: the due-totals of the crews led by the user's person (as their rows show them)
 *  added up, or null when there is no such person or they lead no crew. */
export function useMyCrewDue(sheetId: string, user: string): string | null {
  const people = usePeople(sheetId);
  const crews = useCrews(sheetId);
  const due = usePeopleDue(sheetId);
  const p = findUser(people.data ?? [], user);
  const led = p ? (crews.data ?? []).filter((c) => c.leadId === p.id) : [];
  if (led.length === 0) return null;
  return sum(led.map((c) => crewDueTotal(c, due)));
}

/** The signed-in user's watched total: the due-totals of the people the user watches (as their rows show them) added
 *  up, skipping n/a; 0.00 when the user watches no one. */
export function useWatchedDue(sheetId: string, user: string): string {
  const watches = useWatches(sheetId);
  const due = usePeopleDue(sheetId);
  const watched = new Set((watches.data ?? []).filter((w) => w.user === user).map((w) => w.personId));
  return sum([...watched].map((id) => due.get(id) ?? 'n/a'));
}

/** How many of the sheet's claims have each status. */
export function useStatusCounts(sheetId: string): (status: ClaimStatus) => number {
  const claims = useClaims(sheetId);
  return (status) => (claims.data ?? []).filter((c) => c.status === status).length;
}

/** How many of the sheet's REVIEW claims are assigned to the user for review. */
export function useMyReviewsCount(sheetId: string, user: string): number {
  const claims = useClaims(sheetId);
  return (claims.data ?? []).filter((c) => c.status === 'REVIEW' && c.reviewAssigned === user).length;
}

/** The due-total of each grade: the due-totals of the people on it on the sheet's today (retired people left out)
 *  added up, skipping n/a. */
export function useGradeDue(sheetId: string): (grade: string) => string {
  const people = usePeople(sheetId);
  const due = usePeopleDue(sheetId);
  const late = useLate(sheetId);
  const active = (people.data ?? []).filter((p) => p.retiredFrom === null);
  return (grade) => sum(active.filter((p) => gradeOn(p, late.today) === grade).map((p) => due.get(p.id) ?? 'n/a'));
}
