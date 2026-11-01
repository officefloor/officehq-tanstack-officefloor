import { gradeOn, heldOnBy, retiredOut, usePeople } from '../people/usePeople';
import { claimRateOn, rateOn, totalDue, trimmedTotal, useRates } from '../rates/useRates';
import { useCutTotal } from '../pools/usePools';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { carried, claimed, claimedLive, isOpen, live, overdueFigures, personDueCents, useClaims } from './useClaims';

/**
 * The sheet's totals: its claimed-total, everyone's counted (OPEN or SETTLED) claims added up
 * (held ones too, but not OPEN ones once their person retires, unless stated, nor those that correct another), and its due-total, everyone's due-total
 * added up (a person whose due-total is n/a is skipped), and how many of the claims that count are
 * overdue, with their dues added up. Both due and overdue totals leave out the single highest and
 * lowest person's figure when three or more people have one. Its settled-total and open-total are
 * everyone's due-total before the cap split into SETTLED and OPEN-or-PENDING claims, simply added up.
 * A person's figures carry their parts of the claims shared with them, and their own claims less the holders' parts. Each person's own figures too. One place, so every screen repeating a total shows the same figure.
 */
export function useSheetTotals(sheet: string) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const claims = live(all, heldOnBy(people, today));
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const cap = useCap(sheet);
  const cutOf = useCutTotal(sheet);
  // A claim's rate by its own person's grade on its date.
  const byId = new Map(people.map((p) => [p.id, p]));
  const ownRate = (c: (typeof claims)[number]) => {
    const owner = byId.get(c.personId);
    return owner === undefined ? undefined : claimRateOn(rates, gradeOn(owner, c.date), c);
  };
  // A share holder's part is at the rate of their own grade on the claim's date.
  const holderRate = (c: (typeof claims)[number], id: number) => {
    const holder = byId.get(id);
    return holder === undefined ? undefined : claimRateOn(rates, gradeOn(holder, c.date), c);
  };
  const dues = people.map((p) =>
    personDueCents(carried(claims, p.id), floor, rateOn(rates, gradeOn(p, today), today), ownRate, late, cap, p.id, cutOf(p.id), holderRate),
  );
  // Everyone's due-total before the cap, split into its SETTLED and its OPEN-and-PENDING part.
  const part = (keep: (c: (typeof claims)[number]) => boolean) =>
    people.map((p) =>
      personDueCents(carried(claims, p.id).filter(keep), floor, rateOn(rates, gradeOn(p, today), today), ownRate, late, undefined, p.id, undefined, holderRate),
    );
  const over = people.map((p) =>
    overdueFigures(claims.filter((c) => c.personId === p.id), late, floor, (c) => claimRateOn(rates, gradeOn(p, c.date), c)),
  );
  return {
    /** Each person's own due-total (capped) and overdue-total, in cents, in the people's order. */
    perPerson: people.map((p, i) => ({ person: p, due: dues[i], overdue: over[i].cents })),
    claimedTotal: claimed(claimedLive(all, (id) => people.some((p) => p.id === id && retiredOut(p, today)))),
    dueTotal: trimmedTotal(dues),
    settledTotal: totalDue(part((c) => c.status === 'SETTLED')),
    openTotal: totalDue(part(isOpen)),
    overdueCount: over.reduce((n, o) => n + o.count, 0),
    overdueTotal: trimmedTotal(over.map((o) => o.cents)),
  };
}
