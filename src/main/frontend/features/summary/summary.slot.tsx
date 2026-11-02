import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES, isRetired, usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { hundredths, twoDecimals, useRates } from '../rates/rates';
import { useCapCents, useFloorCents, useReviewSla } from '../settings/settings';
import { CLAIM_STATUSES, isActive, personDueCents, reviewLate, statusCount, useClaims, useLate } from '../claims/claims';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { classicPeriodDueCents, periodDueCents, shownDueTotal, usePeriods } from '../periods/periods';
import { Fig } from '../locale/Fig';

// The sheet at a glance, figured exactly as the lists figure it. Per grade: the people on it today (retired ones
// excluded), their due-totals added up (an n/a due-total adds nothing) and how many of them are active. Per period: its due-total, as its row shows
// it, and its change from the period before it by from-date (none for the earliest). Per status: how many of the sheet's claims have it (every claim, whoever's). Per void reason on the list: how
// many VOID claims were voided with it. And how many REVIEW claims are late for review.
// Beside it, the classic summary: per period, its due-total counting each claim by its status now (a closed period shows
// the figure fixed when it closed), and the sheet's due-total untrimmed (everyone's due-total added up, n/a adding nothing).
function Summary({ sheet }: { sheet: string; user: string }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const late = useLate(sheet);
  const reviewSla = useReviewSla(sheet);
  const { data: voidReasons = [] } = useVoidReasons(sheet);

  const current = people.filter((p) => !isRetired(p));
  const periodTotals = periodDueCents(periods, claims, people, rates, floor, late);
  const shownTotals = [...periods]
    .sort((a, b) => a.from.localeCompare(b.from))
    .map((p) => ({ period: p, shown: shownDueTotal(p, periodTotals.get(p.name) ?? 0n) }));
  const classicTotals = classicPeriodDueCents(periods, claims, people, rates, floor, late);
  const classicDue = people.reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, cap, people) ?? 0n), 0n);
  const periodChanges = new Map(
    shownTotals.map(({ period, shown }, i) => [
      period.name,
      i === 0 ? '' : twoDecimals(hundredths(shown) - hundredths(shownTotals[i - 1].shown)),
    ]),
  );

  return (
    <section>
      <h2>Summary</h2>
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
            const onGrade = current.filter((p) => p.grade === g);
            const due = onGrade.reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, cap, people) ?? 0n), 0n);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`summary-grade-${g}-count`}>{onGrade.length}</td>
                <td data-testid={`summary-grade-${g}-due-total`}><Fig>{twoDecimals(due)}</Fig></td>
                <td data-testid={`summary-grade-${g}-active`}>{onGrade.filter((p) => isActive(claims, p, today)).length}</td>
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
              <td data-testid={`summary-period-${p.name}-due-total`}><Fig>{shownDueTotal(p, periodTotals.get(p.name) ?? 0n)}</Fig></td>
              <td data-testid={`summary-period-${p.name}-change`}><Fig>{periodChanges.get(p.name)}</Fig></td>
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
          {CLAIM_STATUSES.map((s) => (
            <tr key={s}>
              <td>{s}</td>
              <td data-testid={`summary-status-${s}-count`}>{statusCount(claims, s)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Late for review: <strong data-testid="summary-review-late-count">{claims.filter((c) => reviewLate(c, reviewSla, today)).length}</strong>
      </p>
      <h3>Classic summary</h3>
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
              <td data-testid={`classic-period-${p.name}-due-total`}><Fig>{shownDueTotal(p, classicTotals.get(p.name) ?? 0n)}</Fig></td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Due total: <strong data-testid="classic-due-total"><Fig>{twoDecimals(classicDue)}</Fig></strong>
      </p>
      <table>
        <thead>
          <tr>
            <th>Void reason</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {voidReasons.map((r) => (
            <tr key={r.reason}>
              <td>{r.reason}</td>
              <td data-testid={`summary-void-${r.reason}-count`}>{r.voided}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 30, Component: Summary });
