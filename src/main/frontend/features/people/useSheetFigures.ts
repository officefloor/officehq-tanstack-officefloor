import { countsLive, hasTag, isOpen, useClaims, useTags } from '../claims/useClaims';
import { isOverdue } from '../claims/overdue';
import { useGrace } from '../claims/useGrace';
import { capped, claimDue, dueTotal, excess, formatCents, toCents } from '../rates/due';
import { rateOn, useRates } from '../rates/useRates';
import { useCap, useFloor, useLatePct, usePolicyDate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { gradeOn, heldOn, retiredOn, usePeople, type Person } from './usePeople';

export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The figures the people list, its totals, the summary and the header are all worked from, so
// every place that repeats a figure shows the same one.
export function useSheetFigures(sheet: string) {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const tags = useTags(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const grace = useGrace(sheet);
  const late = { pct: useLatePct(sheet), today, grace };
  const policy = usePolicyDate(sheet);
  const cap = useCap(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  const live = claims.filter((c) => {
    const person = byId.get(c.personId);
    return person !== undefined && countsLive(c, (date) => heldOn(person, date) || retiredOn(person, today), today);
  });
  // A claim is worked at the grade in force on its date; the row shows the one in force today.
  // The sum of a person's figures, before the cap.
  const uncappedOf = (person: Person) =>
    dueTotal(
      live.filter((c) => c.personId === person.id),
      (date) => rateOn(rates, gradeOn(person, date), date),
      rateOn(rates, gradeOn(person, today), today),
      floor,
      late,
      policy,
    );
  // A person's due-total: at most the sheet's cap (n/a is not capped); everything that adds up
  // due-totals uses it. Their excess is how far the sum of their figures is above the cap.
  const totalOf = (person: Person) => capped(uncappedOf(person), cap);
  const excessOf = (person: Person) => excess(uncappedOf(person), cap);
  // A person's settled-total and open-total: the dues of their counting SETTLED claims (frozen
  // figures, corrections included), and of their counting OPEN and PENDING claims; n/a adds nothing.
  // Together they make the sum of their figures before the cap.
  const dueSumOf = (person: Person, which: (c: (typeof live)[number]) => boolean) =>
    live
      .filter((c) => c.personId === person.id && which(c))
      .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late, policy) ?? 0n), 0n);
  const settledTotalOf = (person: Person) => dueSumOf(person, (c) => c.status === 'SETTLED');
  const openTotalOf = (person: Person) => dueSumOf(person, isOpen);
  // The sheet's settled-total and open-total: everyone's figure added up, not trimmed.
  const sheetSettledTotal = formatCents(people.reduce((sum, p) => sum + settledTotalOf(p), 0n));
  const sheetOpenTotal = formatCents(people.reduce((sum, p) => sum + openTotalOf(p), 0n));
  const overdue = live.filter((c) => isOverdue(c, today, grace));
  const overdueSum = (cs: typeof overdue) =>
    cs.reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n);
  const overdueTotal = (cs: typeof overdue) => formatCents(overdueSum(cs));
  // The sheet's due-total: the people's due-totals added up, skipping n/a, trimmed.
  const sheetDueTotal = formatCents(trimmedSum(people.map(totalOf).filter((due): due is bigint => due !== null)));
  // The sheet's overdue-total: each person's overdue-total (always a figure) added up, trimmed.
  const sheetOverdueTotal = formatCents(trimmedSum(people.map((p) => overdueSum(overdue.filter((c) => c.personId === p.id)))));
  // Active: an OPEN or PENDING claim that counts, dated within the 30 days up to and including today.
  const activeFrom = today === undefined ? undefined : daysBefore(today, 29);
  const isActive = (person: Person) =>
    activeFrom !== undefined &&
    live.some((c) => c.personId === person.id && isOpen(c) && c.date >= activeFrom && c.date <= today!);
  // Settled: the sum of a person's SETTLED claims' frozen figures, dated within the 90 days up to and including today.
  const settledFrom = today === undefined ? undefined : daysBefore(today, 89);
  const recentSettledOf = (person: Person) =>
    settledFrom === undefined
      ? 0n
      : claims
          .filter((c) => c.personId === person.id && c.status === 'SETTLED' && c.date >= settledFrom && c.date <= today! && /^-?\d+\.\d{2}$/.test(c.due))
          .reduce((sum, c) => sum + toCents(c.due), 0n);
  // Aged: the dues of a person's OPEN and PENDING claims that count, split by the claim's age on
  // today (days from its date): 0 to 30, 31 to 60, 61 or more; a claim dated after today is 0 to 30.
  const agedOf = (person: Person) => {
    const buckets = [0n, 0n, 0n];
    if (today === undefined) return buckets;
    for (const c of live) {
      if (c.personId !== person.id || !isOpen(c)) continue;
      const age = daysBetween(c.date, today);
      const due = claimDue(c, rateOn(rates, gradeOn(person, c.date), c.date), floor, late, policy) ?? 0n;
      buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2] += due;
    }
    return buckets;
  };
  // Each tag ever used on the sheet with its due-total: the dues of the counting claims that carry it
  // (a claim with two tags counts in both); n/a adds nothing.
  const tagTotals = tags.map((tag) => ({
    tag,
    total: formatCents(
      live
        .filter((c) => hasTag(c, tag))
        .reduce((sum, c) => sum + (claimDue(c, rateOn(rates, gradeOn(byId.get(c.personId)!, c.date), c.date), floor, late, policy) ?? 0n), 0n),
    ),
  }));
  const onSheet = new Set(people.map((p) => p.id));
  const sheetClaims = claims.filter((c) => onSheet.has(c.personId));
  const statusCount = (s: (typeof STATUSES)[number]) => sheetClaims.filter((c) => c.status === s).length;
  return { people, claims, rates, today, byId, live, totalOf, excessOf, overdue, overdueTotal, sheetDueTotal, sheetOverdueTotal, statusCount, isActive, recentSettledOf, agedOf, settledTotalOf, openTotalOf, sheetSettledTotal, sheetOpenTotal, tagTotals };
}

// People's figures added up, leaving out the single highest and the single lowest when there are
// three or more.
function trimmedSum(figures: bigint[]): bigint {
  const sum = figures.reduce((a, b) => a + b, 0n);
  if (figures.length < 3) return sum;
  const max = figures.reduce((a, b) => (b > a ? b : a));
  const min = figures.reduce((a, b) => (b < a ? b : a));
  return sum - max - min;
}

// The ISO date `days` days before `date`.
function daysBefore(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - days);
  return d.toISOString().slice(0, 10);
}

// The whole days from one ISO date to a later one (negative when it is earlier).
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}
