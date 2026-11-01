import { live, personDueCents, useClaims } from '../claims/useClaims';
import { GRADES, gradeOn, heldOn, usePeople } from '../people/usePeople';
import { claimRateOn, rateOn, useRates } from '../rates/useRates';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/**
 * Per grade, the people not retired on it on the sheet's today, each with their due-total in cents
 * (null when n/a), worked out as the list works it out. One place, so the summary and the
 * consolidated figures agree.
 */
export function useGradeDues(sheet: string) {
  const { data: all = [] } = useClaims(sheet);
  const { data: everyone = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const cap = useCap(sheet);
  const people = everyone.filter((p) => !p.retired);
  const dueOf = (p: (typeof people)[number]) =>
    personDueCents(
      live(all.filter((c) => c.personId === p.id), (_, date) => heldOn(p, date, today)),
      floor,
      rateOn(rates, gradeOn(p, today), today),
      (c) => claimRateOn(rates, gradeOn(p, c.date), c),
      late,
      cap,
    );
  return Object.fromEntries(
    GRADES.map((g) => {
      const on = people.filter((p) => gradeOn(p, today) === g);
      return [g, on.map((p) => ({ person: p, due: dueOf(p) }))];
    }),
  ) as Record<(typeof GRADES)[number], Array<{ person: (typeof people)[number]; due: bigint | null }>>;
}
