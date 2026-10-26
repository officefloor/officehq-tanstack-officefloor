import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims } from '../claims/useClaims';
import { gradeOn, useHeld, usePeople } from '../people/people.slot';
import { useFloorCents, useLate } from '../access/useAccess';
import { showPeriodDueTotal, usePeriodDueTotal, usePeriods } from '../periods/periods.slot';
import { useIsActive } from '../active/active.slot';
import { useDueTotals } from '../rates/rates.slot';
import { claimDueCents, showHundredths, toHundredths, useRates } from '../rates/useRates';
import { useVoidReasons } from '../voidreasons/voidReasons.slot';
import { useIsReviewLate } from '../reviewsla/reviewSla.slot';

const GRADES = ['A', 'B', 'C'] as const;
export const STATUSES = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'] as const;

// The summary panel, built from the same figures as the lists so it always agrees with them. Per grade: the people
// on it today, retired people left out, and their due-totals added up (skipping those that are n/a). Per period:
// its due-total as its row shows it, and its change from the period before it by start date (none for the earliest). Per grade too: how many of its people are active. Per status: the claims on the sheet with that status. And the REVIEW claims past the review SLA.
// Per void reason on the list: the VOID claims voided with it.
function SummaryPanel({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const totals = useDueTotals(sheetId);
  const periods = usePeriods(sheetId);
  const periodDueTotal = usePeriodDueTotal(sheetId);
  const claims = useClaims(sheetId);
  const isActive = useIsActive(sheetId);
  const voidReasons = useVoidReasons(sheetId);
  const isReviewLate = useIsReviewLate(sheetId);

  const current = (people.data ?? []).filter((p) => p.retiredFrom === null);
  const periodShown = new Map((periods.data ?? []).map((p) => [p.id, showPeriodDueTotal(p, periodDueTotal(p))]));
  const periodChange = new Map<number, string>();
  const byStart = [...(periods.data ?? [])].sort((a, b) => a.from.localeCompare(b.from));
  byStart.forEach((p, i) => {
    const prev = byStart[i - 1];
    periodChange.set(p.id, prev
      ? showHundredths(toHundredths(periodShown.get(p.id)!) - toHundredths(periodShown.get(prev.id)!))
      : '');
  });
  return (
    <section data-testid="summary-panel">
      <h2>Summary</h2>
      <table data-testid="summary-grades">
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
            let sum = 0n;
            for (const p of on) sum += totals.get(p.id) ?? 0n;
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{on.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}>{showHundredths(sum)}</td>
                <td data-testid={`summary-grade-${g}-active`}>{on.filter((p) => isActive(p.id)).length}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table data-testid="summary-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
            <th>Change</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`summary-period-${p.name}-due-total`}>{periodShown.get(p.id)}</td>
              <td data-testid={`summary-period-${p.name}-change`}>{periodChange.get(p.id)}</td>
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
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{(claims.data ?? []).filter((c) => c.status === s).length}</td>
            </tr>
          ))}
          <tr>
            <td>Review late</td>
            <td data-testid="summary-review-late-count">{(claims.data ?? []).filter(isReviewLate).length}</td>
          </tr>
        </tbody>
      </table>
      <table data-testid="summary-void-reasons">
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {(voidReasons.data ?? []).map((r) => (
            <tr key={r.reason}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>
                {(claims.data ?? []).filter((c) => c.status === 'VOID' && c.voidReason === r.reason).length}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// The classic summary, shown beside the summary. Per period: the due figures of the claims whose due-by falls in it,
// each counted by its status now (OPEN, PENDING and SETTLED count; held or deferred ones left out as in the live
// totals), added up; a closed or soft-closed period shows the figure fixed then. And the sheet's due-total with no
// trimming: everyone's due-total added up, skipping those that are n/a.
function ClassicSummaryPanel({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const totals = useDueTotals(sheetId);
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const held = useHeld(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);

  const personOf = new Map((people.data ?? []).map((p) => [p.id, p]));
  const liveTotal = (from: string, to: string): bigint => {
    let sum = 0n;
    for (const c of claims.data ?? []) {
      if (c.dueBy < from || c.dueBy > to || !counts(c, held)) continue;
      sum += claimDueCents(rates.data ?? [], gradeOn(personOf.get(c.personId), c.date), c, floor, late) ?? 0n;
    }
    return sum;
  };
  let sheetTotal = 0n;
  for (const t of totals.values()) if (t != null) sheetTotal += t;
  return (
    <section data-testid="classic-summary-panel">
      <h2>Classic summary</h2>
      <table data-testid="classic-periods">
        <thead>
          <tr>
            <th>Period</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td data-testid={`classic-period-${p.name}-due-total`}>{showPeriodDueTotal(p, liveTotal(p.from, p.to))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due total: <strong data-testid="classic-due-total">{showHundredths(sheetTotal)}</strong>
      </p>
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 30, Component: SummaryPanel }),
  SheetBody.fill({ order: 31, Component: ClassicSummaryPanel }),
];
