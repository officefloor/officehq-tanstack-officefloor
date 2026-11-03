import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** Per calendar month (yyyy-mm) any claim on the sheet is dated in, its due-total: the dues of the
 *  claims dated in it that count (as in a person's due-total) added up, skipping 'n/a'. */
function MonthTotals({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const holds = holdsById(people.data, today);
  const months = [...new Set((claims.data ?? []).map((c) => c.date.slice(0, 7)))].sort();
  const dues = new Map<string, string[]>(months.map((m) => [m, []]));
  for (const c of claims.data ?? []) {
    if (!counts(c, holds, today)) continue;
    const person = people.data?.find((p) => p.id === c.personId);
    const grade = gradeOn(person, c.date);
    dues.get(c.date.slice(0, 7))!.push(claimDueNow(c, claims.data, rateFor(rates.data, grade, c.date, c.added), settings, today, grade));
  }
  return (
    <section data-testid="month-totals">
      <table>
        <thead>
          <tr>
            <th>Month</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {months.map((m) => (
            <tr key={m}>
              <td>{m}</td>
              <td data-testid={`month-${m}-due-total`}>{sumDues(dues.get(m)!)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 42, Component: ({ sheetId }) => <MonthTotals sheetId={sheetId} /> });
