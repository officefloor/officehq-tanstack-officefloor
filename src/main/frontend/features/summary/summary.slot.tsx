import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDueTotal, usePeriods } from '../periods/periods';
import { personDueTotal, sumDue, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** The statuses the summary counts claims by, each shown even when no claim has it. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

/** The sheet's summary, every figure as the list shows it: per grade the people on it today (those retired on
 *  or before the sheet's today left out) and their due-totals added up (skipping n/a), per period its
 *  due-total, and per status how many of the sheet's claims have it. */
function Summary({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const active = people.filter((p) => p.retiredFrom === null || p.retiredFrom > today);

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
                    {sumDue(onGrade.map((p) => personDueTotal(p, claims, rates, today, floor, late)))}
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
                Claims <span data-testid={`summary-status-${s}-count`}>{claims.filter((c) => (c.status as string) === s).length}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 23, Component: Summary });
