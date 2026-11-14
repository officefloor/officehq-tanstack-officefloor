import { isActive } from '../active/active';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { GRADES, liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { periodDue, usePeriods } from '../periods/periods';
import { personDue, total, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules, useSettings } from '../settings/settings';
import type { DueRules } from '../settings/settings';
import { reviewLate } from '../reviewlate/reviewLate';
import { useVoidReasons } from '../voidreasons/voidReasons';

/** The statuses the summary counts claims by; one no claim reaches yet still shows, as 0. */
const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const;

// The sheet at a glance, worked out from the same figures the list shows. Per grade: the people on it
// today (retired people left out), their due-totals added up (skipping n/a) and how many of them are active. Per period: its
// due-total and its change from the period before it by start date (none for the earliest). Per status: how many claims have it, and how many REVIEW claims are late past the review SLA. Per void reason on the list: how many VOID claims were voided with it.
export function Summary() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const periods = usePeriods().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const voidReasons = useVoidReasons().data ?? [];
  const reviewSla = useSettings().data?.reviewSla ?? '';

  if (!user || !sheet) return null;

  const current = people.filter((p) => !p.retired);
  const periodDues = new Map(periods.map((p) => [p.name, periodDue(p, claims, people, rates, rules)]));
  const byStart = [...periods].sort((a, b) => a.from.localeCompare(b.from));
  const periodChange = new Map(
    byStart.map((p, i) => [
      p.name,
      i === 0 ? '' : (cents(periodDues.get(p.name)!) - cents(periodDues.get(byStart[i - 1].name)!)) / 100,
    ]),
  );
  return (
    <section data-testid="summary">
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>People</th>
            <th>Due total</th>
            <th>Active</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const on = current.filter((p) => p.grade === g);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{on.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{gradeDueTotal(g, people, claims, rates, rules)}</td>
                <td data-testid={`summary-grade-${g}-active`}>
                  {on.filter((p) => isActive(claims.filter((c) => c.personId === p.id), liveHolds(p, rules.today), rules.today)).length}
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
            <th>Due total</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodDues.get(p.name)}</td>
              <td data-testid={`summary-period-${p.name}-change`}>{formatChange(periodChange.get(p.name))}</td>
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
          <tr>
            <td>REVIEW late</td>
            <td data-testid="summary-review-late-count">
              {claims.filter((c) => reviewLate(c, reviewSla, rules.today)).length}
            </td>
          </tr>
        </tbody>
      </table>
      <table>
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {voidReasons.map((r) => (
            <tr key={r}>
              <td>{r}</td>
              <td data-testid={`summary-void-${r}-count`}>
                {claims.filter((c) => c.status === 'VOID' && c.voidReason === r).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** The due-total of a grade: the due-totals of the people on it today (retired people left out) added up, skipping n/a. */
export function gradeDueTotal(grade: string, people: Person[], claims: Claim[], rates: Rate[], rules: DueRules): string {
  const dues = people
    .filter((p) => !p.retired && p.grade === grade)
    .map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, { personId: p.id, claims, people }));
  return total(dues);
}

/** A two-decimal value as whole hundredths, exactly. */
const cents = (value: string) => Math.round(Number(value) * 100);

/** A change in due-total, two decimals with a minus sign when lower; nothing for the earliest period. */
function formatChange(change: number | string | undefined): string {
  return typeof change === 'number' ? (change === 0 ? '0.00' : change.toFixed(2)) : '';
}
