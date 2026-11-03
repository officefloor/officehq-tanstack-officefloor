import { isOpen, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, usePeople } from '../people/usePeople';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { hundredths, money, rateFor, useRates } from './useRates';

/** A candidate rate's percent: 0.00 or more with up to two decimals (below 100000), as the rate form takes it. */
const PCT = /^(\d{1,5}(\.\d{1,2})?|\.\d{1,2})$/;

/** What a candidate rate would change: how many open (OPEN or PENDING) claims of people on the grade (the grade
 *  they had on the claim's date) would show a different due now were that rate the grade's on every date, and
 *  the sum of new due minus current due. A claim a statement binds keeps its figure; a settled one is not open.
 *  Returns a function computing it from the sheet as loaded; it throws on a bad percent. Saves nothing. */
export function useRatePreview(sheetId: string): (grade: string, pct: string) => { count: number; diff: string } {
  const people = usePeople(sheetId).data;
  const claims = useClaims(sheetId).data;
  const rates = useRates(sheetId).data;
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  return (grade, pct) => {
    const candidate = pct.trim();
    if (candidate === '') throw new Error('rate is required');
    if (!PCT.test(candidate)) throw new Error('rate must be a percent of 0.00 or more with up to two decimals');
    const asPct = money(hundredths(candidate.startsWith('.') ? `0${candidate}` : candidate));
    let count = 0;
    let diff = 0n;
    for (const c of claims ?? []) {
      if (!isOpen(c)) continue;
      const p = (people ?? []).find((x) => x.id === c.personId);
      const g = gradeOn(p, c.date);
      if (g !== grade) continue;
      const now = claimDueNow(c, claims, rateFor(rates, g, c.date, c.added), settings, today, g);
      const next = claimDueNow(c, claims, asPct, settings, today, g);
      if (now === next) continue;
      count++;
      diff += (next === 'n/a' ? 0n : hundredths(next)) - (now === 'n/a' ? 0n : hundredths(now));
    }
    return { count, diff: money(diff) };
  };
}
