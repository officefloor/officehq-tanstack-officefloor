import { useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import { periodDue, usePeriods } from '../periods/periods';
import { personDue, total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';

/** The statuses the summary counts claims by; one no claim reaches yet still shows, as 0. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'] as const;

// The sheet at a glance, worked out from the same figures the list shows. Per grade: the people on it
// today (retired people left out) and their due-totals added up (skipping n/a). Per period: its
// due-total. Per status: how many claims have it.
export function Summary() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const periods = usePeriods().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const active = people.filter((p) => !p.retired);
  return (
    <section data-testid="summary">
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const on = active.filter((p) => p.grade === g);
            const dues = on.map((p) =>
              personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, p.held, rules),
            );
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{on.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{total(dues)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDue(p, claims, people, rates, rules)}</td>
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
