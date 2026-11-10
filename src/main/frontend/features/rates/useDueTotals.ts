import { type Claim, type ClaimShare, counts, fixedDue, sharePart, useClaims } from '../claims/useClaims';
import { useCutTotals } from '../cuts/useCutTotals';
import { claimDueNow, roundsHalfUp } from '../overdue/useOverdue';
import { gradeOn, holdsById, type Person, usePeople } from '../people/usePeople';
import { type Settings, settingOn, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { hundredths, money, type Rate, rateFor, sumDues, useRates } from './useRates';

/** Each person's due-total, by person id: the sum of the figures of their claims that count (open (OPEN or PENDING)
 *  claims left out once their retirement date is reached (a hold leaves none out) unless a statement binds them; a settled claim's frozen due, else the figure their last statement
 *  bound it to, else its live due, with its late charge while overdue),
 *  skipping those with none; 'n/a' when no rate for the grade they have on the sheet's today is in force
 *  then and no counting claim of theirs keeps a fixed figure or has a due (at the grade it was claimed under).
 *  A shared claim counts its due less its holders' parts for its own person, and each holder's part (see sharePart: at the
 *  rate of their own grade on the claim's date, frozen once settled; 0.00 when they have no rate) for that holder.
 *  A due-total is at most the sheet's cap in force on its today less their cut total, not below 0.00 ('n/a' is not capped). */
export function useDueTotals(sheetId: string): Map<number, string> {
  const totals = new Map<number, string>();
  for (const [id, f] of useDueFigures(sheetId)) totals.set(id, f.total);
  return totals;
}

/** Each person's excess, by person id: how far the sum of their figures plus their cut total is above the sheet's cap; 0.00 when
 *  it is not (or there is no cap, or their due-total is 'n/a'). */
export function useExcess(sheetId: string): Map<number, string> {
  const excess = new Map<number, string>();
  for (const [id, f] of useDueFigures(sheetId)) excess.set(id, f.excess);
  return excess;
}

/** Each person's settled-total and open-total, by person id: the sum of the figures of their counting SETTLED
 *  claims, and of their counting open (OPEN or PENDING) claims, as in their due-total ('n/a' figures skipped).
 *  The two add up to the due-total before the cap. */
export function useSettledOpenTotals(sheetId: string): Map<number, { settled: string; open: string }> {
  const split = new Map<number, { settled: string; open: string }>();
  for (const [id, f] of useDueFigures(sheetId)) split.set(id, { settled: f.settled, open: f.open });
  return split;
}

/** Each person's score figures, by person id: the sum of the figures of their counting SETTLED claims that
 *  correct nothing ('n/a' figures skipped), whole whatever their shares. */
export function useScoreSettled(sheetId: string): Map<number, string> {
  const scored = new Map<number, string>();
  for (const [id, f] of useDueFigures(sheetId)) scored.set(id, f.scored);
  return scored;
}

/** Each person's due-total held to the cap, their excess over it, its settled and open parts, and the settled
 *  figures their score counts. */
function useDueFigures(sheetId: string): Map<number, { total: string; excess: string; settled: string; open: string; scored: string }> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const settings = useSettings(sheetId).data;
  const cuts = useCutTotals(sheetId);
  return dueFigures(people.data, claims.data, rates.data, settings, today.data?.today, cuts);
}

/** Each person's due-total, by person id, from these people, claims, rates and settings on this today (as
 *  useDueTotals shows them). */
export function dueTotalsOf(...args: Parameters<typeof dueFigures>): Map<number, string> {
  const totals = new Map<number, string>();
  for (const [id, f] of dueFigures(...args)) totals.set(id, f.total);
  return totals;
}

function dueFigures(
  people: Person[] | undefined,
  claims: Claim[] | undefined,
  rates: Rate[] | undefined,
  settings: Settings | undefined,
  today: string | undefined,
  cuts?: Map<number, string>,
): Map<number, { total: string; excess: string; settled: string; open: string; scored: string }> {
  const holds = holdsById(people, today);
  const cap = settingOn(settings, 'cap', today);
  const totals = new Map<number, { total: string; excess: string; settled: string; open: string; scored: string }>();
  // Each counting claim's due now, by claim id, at its own person's grade.
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  const dueOf = new Map<number, string>();
  for (const c of claims ?? []) {
    const owner = byId.get(c.personId);
    if (owner === undefined || !counts(c, holds, today)) continue;
    dueOf.set(c.id, claimDueNow(c, claims, rateFor(rates, gradeOn(owner, c.date), c.date, c.added), settings, today, gradeOn(owner, c.date)));
  }
  // A holder's part of a claim, at the rate of their own grade in force on its date ('n/a' when none: 0.00).
  const partOf = (c: Claim, s: ClaimShare): string => {
    const holder = byId.get(s.personId);
    const grade = holder === undefined ? undefined : gradeOn(holder, c.date);
    return sharePart(c, s.share, rateFor(rates, grade, c.date, c.added), roundsHalfUp(c, claims, settings, today));
  };
  for (const p of people ?? []) {
    const own = (claims ?? []).filter((c) => c.personId === p.id && dueOf.has(c.id));
    // A shared claim's own person counts its due less its holders' parts; each holder counts their part.
    const ownDues = own.map((c) => {
      const d = dueOf.get(c.id)!;
      if (d === 'n/a' || c.shares.length === 0) return d;
      return money(hundredths(d) - c.shares.reduce((sum, s) => {
        const part = partOf(c, s);
        return sum + (part === 'n/a' ? 0n : hundredths(part));
      }, 0n));
    });
    const held = (claims ?? []).filter((c) => c.personId !== p.id && dueOf.has(c.id) && dueOf.get(c.id) !== 'n/a'
      && c.shares.some((s) => s.personId === p.id));
    const counting = [...own, ...held];
    const dues = [...ownDues, ...held.map((c) => partOf(c, c.shares.find((s) => s.personId === p.id)!))];
    const rated = rateFor(rates, p.grade, today) !== undefined;
    const shown = rated || own.some((c) => fixedDue(c) !== null && fixedDue(c) !== 'n/a') || dues.some((d) => d !== 'n/a');
    const sum = sumDues(dues);
    // The cap covers the due-total and the cut total together: the due-total is held to the cap less the cut.
    const cut = hundredths(cuts?.get(p.id) ?? '0.00');
    const room = cap === null ? 0n : hundredths(cap) - cut > 0n ? hundredths(cap) - cut : 0n;
    const over = shown && cap !== null && hundredths(sum) > room;
    totals.set(p.id, {
      total: !shown ? 'n/a' : over ? money(room) : sum,
      excess: over ? money(hundredths(sum) - room) : '0.00',
      settled: sumDues(dues.filter((_, i) => counting[i].status === 'SETTLED')),
      open: sumDues(dues.filter((_, i) => counting[i].status !== 'SETTLED')),
      scored: sumDues(own.filter((c) => c.status === 'SETTLED' && c.corrects === null).map((c) => dueOf.get(c.id)!)),
    });
  }
  return totals;
}
