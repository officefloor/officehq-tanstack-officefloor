import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { figure, ratesQuery, sumDue } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';

/**
 * A due-total per calendar month (yyyy-mm), for every month any claim on the sheet is dated in, oldest first: the
 * figures of the claims that count dated in it, added up (n/a skipped; not capped). 0.00 when none of them count.
 */
function MonthTotals({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const months = [...new Set(claims.map((c) => c.date.slice(0, 7)))].sort();
  const counting = claims.filter((c) => counts(c, people, today));

  return (
    <table data-testid="month-totals">
      <thead>
        <tr>
          <th>Month</th>
          <th>Due</th>
        </tr>
      </thead>
      <tbody>
        {months.map((m) => (
          <tr key={m} data-testid={`month-${m}`}>
            <td>{m}</td>
            <td data-testid={`month-${m}-due-total`}>
              {sumDue(counting
                .filter((c) => c.date.startsWith(m))
                .map((c) => figure(c, rates, people.find((p) => p.id === c.personId), floor, late)))}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = SheetPanel.fill({ order: 26.5, Component: MonthTotals });
