import { SheetBody } from '../../slots/defs/sheet';
import { STATUSES, active as isActive, useClaims } from '../claims/claims';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { periodDueTotal, usePeriods } from '../periods/periods';
import { diffDue, personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { useVoidReasons } from '../voidreasons/voidreasons';

/** The sheet's summary, every figure as the list shows it: per grade the people on it today (those retired left
 *  out, whatever their retire date) and their due-totals added up (skipping n/a), per period its
 *  due-total and its change from the period before it by start date (none for the earliest), how many of them are active (see claims' active), and per status how many of the sheet's claims have it, and per void reason on the list how many VOID claims were
 *  voided with it. */
function Summary({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const voidReasons = useVoidReasons(sheetId).data ?? [];
  const active = people.filter((p) => p.retiredFrom === null);
  const periodTotals = new Map(periods.map((p) => [p.name, periodDueTotal(p, claims, people, rates, floor, late)]));
  const byStart = [...periods].sort((a, b) => a.from.localeCompare(b.from));
  const periodChange = new Map(byStart.map((p, i) => [p.name, i === 0 ? ''
    : diffDue(periodTotals.get(p.name)!, periodTotals.get(byStart[i - 1].name)!)]));

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
                <td>
                  Active{' '}
                  <span data-testid={`summary-grade-${g}-active`}>
                    {onGrade.filter((p) => isActive(p, claims, today)).length}
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
                  {periodTotals.get(p.name)}
                </span>
              </td>
              <td>
                Change <span data-testid={`summary-period-${p.name}-change`}>{periodChange.get(p.name)}</span>
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
          {voidReasons.map((r) => (
            <tr key={r.id} data-testid={`summary-void-${r.reason}`}>
              <th>Voided: {r.reason}</th>
              <td>
                Claims <span data-testid={`summary-void-${r.reason}-count`}>{r.voided}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 23, Component: Summary });
