import { SheetBody } from '../../slots/defs/sheet';
import { STATUSES, useClaims } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDueTotal, usePeriods } from '../periods/periods';
import { personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** The sheet's summary, every figure as the list shows it: per grade the people on it today (those retired left
 *  out, whatever their retire date) and their due-totals added up (skipping n/a), per period its
 *  due-total, and per status how many of the sheet's claims have it. */
function Summary({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const active = people.filter((p) => p.retiredFrom === null);

  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g} data-testid={`summary-grade-${g}`}>
                <th>Grade {g}</th>
                <td>
                  People <span data-testid={`summary-grade-${g}-count`}>{onGrade.length}</span>
                </td>
                <td>
                  Due total{' '}
                  <span data-testid={`summary-grade-${g}-due-total`}>
                    {sumDue(onGrade.map((p) => personDueTotal(p, claims, rates, today, floor, late, cap)))}
                  </span>
                </td>
              </tr>
            );
          })}
          {periods.map((p) => (
            <tr key={p.name} data-testid={`summary-period-${p.name}`}>
              <th>Period {p.name}</th>
              <td>
                Due total{' '}
                <span data-testid={`summary-period-${p.name}-due-total`}>
                  {periodDueTotal(p, claims, people, rates, floor, late)}
                </span>
              </td>
            </tr>
          ))}
          {STATUSES.map((s) => (
            <tr key={s} data-testid={`summary-status-${s}`}>
              <th>{s}</th>
              <td>
                Claims <span data-testid={`summary-status-${s}-count`}>{claims.filter((c) => c.status === s).length}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 23, Component: Summary });
