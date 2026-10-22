import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { sheetMonthSum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { useClaims } from './claims';

/** The sheet's due-total per calendar month (yyyy-mm), for every month any claim on it is dated in: the dues of the
 *  claims dated in it that count (see sheetMonthSum). */
function MonthTotals({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const months = [...new Set(claims.map((c) => c.date.slice(0, 7)))].sort();
  if (months.length === 0) return null;
  return (
    <p>
      Due by month{' '}
      {months.map((month) => (
        <span key={month}>
          {month} <strong data-testid={`month-${month}-due-total`}>{sheetMonthSum(people, claims, rates, today, floor, late, month)}</strong>{' '}
        </span>
      ))}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 21.7, Component: MonthTotals });
