import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import type { Claim, ClaimStatus } from '../claims/claimsQuery';
import { GRADES, gradeOn, peopleQuery } from '../people/peopleQuery';
import { periodDue, periodsQuery } from '../periods/periodsQuery';
import { personDue, ratesQuery, sumDue } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';

export const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** How many of the sheet's claims have the status. */
export const statusCount = (claims: Claim[], status: ClaimStatus) => claims.filter((c) => c.status === status).length;

/**
 * The sheet at a glance, worked out from the same figures the lists show. Per grade: the people on it
 * today (retired people excluded) and their due-totals added up, skipping n/a. Per period: its
 * due-total. Per status: how many claims have it.
 */
function Summary({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const active = people.filter((p) => p.retiredFrom === null);

  return (
    <section data-testid="summary">
      <h3>Summary</h3>
      <table data-testid="summary-grades">
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => gradeOn(p, today) === g);
            return (
              <tr key={g} data-testid={`summary-grade-${g}`}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>
                  {sumDue(onGrade.map((p) => personDue(p, people, claims, rates, today, floor, late)))}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table data-testid="summary-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`summary-period-${p.name}`}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDue(p, claims, people, rates, floor, late)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="summary-statuses">
        <thead>
          <tr>
            <th>Status</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s} data-testid={`summary-status-${s}`}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims, s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanel.fill({ order: 30, Component: Summary });
