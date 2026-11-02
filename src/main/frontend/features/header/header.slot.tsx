import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/useOverdue';
import { usePeople } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues } from '../rates/useRates';
import { countWithStatus, STATUSES } from '../summary/statusCounts';

/** The sheet's header counters, shown at all times: how many people are on the sheet (retired people
 *  included), how many claims have each status (as the summary counts them), and the sheet's
 *  due-total and overdue-total (as the totals show them). */
function Header({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const dueTotals = useDueTotals(sheetId);
  const overdue = [...useOverdue(sheetId).values()];
  return (
    <header data-testid="header">
      People <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          · {s} <strong data-testid={`header-claims-${s}-count`}>{countWithStatus(claims.data, s)}</strong>
        </span>
      ))}{' '}
      · Due total <strong data-testid="header-due-total">{sumDues([...dueTotals.values()])}</strong> · Overdue
      total <strong data-testid="header-overdue-total">{sumDues(overdue.map((f) => f.total))}</strong>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -10, Component: ({ sheetId }) => <Header sheetId={sheetId} /> });
