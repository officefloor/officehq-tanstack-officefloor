import { HomeMain } from '../../slots/defs/homeMain';
import { usePeople } from '../people/people';
import { useCurrentSheet } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { live, useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { dueTotal, personMonthDue, useRates } from './rates';
import { useFigure } from '../locale/locale';

// For every calendar month (yyyy-mm) any claim on the sheet is dated in, its due-total: the dues of the
// claims that count dated in it, everyone's added up, not capped or trimmed.
function MonthTotals() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const months = [...new Set((claims.data ?? []).map((c) => c.date.slice(0, 7)))].sort();
  const total = (month: string) =>
    dueTotal(
      (people.data ?? []).map((p) =>
        personMonthDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), month, rates.data, p.grades, today, floor,
          lateOf(settings, today, [p])),
      ),
    );
  if (months.length === 0) return null;
  return (
    <p>
      Due by month{' '}
      {months.map((month) => (
        <span key={month}>
          {month} <strong data-testid={`month-${month}-due-total`}>{fig(total(month))}</strong>{' '}
        </span>
      ))}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 36.6, Component: MonthTotals });
