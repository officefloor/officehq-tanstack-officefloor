import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';
import { GRADES, liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDueTotal, sumDue, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { periodDueTotal, usePeriods } from '../periods/periods';
import { isActive } from '../active/active';

// Every status the summary counts.
const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

// The sheet's summary, worked from the same figures as the lists: per grade, the people on it today
// (retired people left out), their due-totals added up and how many of them are active; per
// period, its due-total; per status, how many claims have it.
function Summary({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);

  const active = people.filter((p) => !p.retired);
  const claimsOf = (p: Person) => claims.filter((c) => c.personId === p.id);
  const dueOf = (p: Person) => personDueTotal(rates, p.grades, today, claimsOf(p), liveHolds(p, today), floor, late, cap);

  return (
    <section data-testid="summary">
      <h2>Summary</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const onGrade = active.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{sumDue(onGrade.map(dueOf))}</td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {onGrade.filter((p) => isActive(claimsOf(p), liveHolds(p, today), today)).length}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>
                {periodDueTotal(p, people, claims, rates, floor, late)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{claims.filter((c) => c.status === s).length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 30, Component: Summary });
