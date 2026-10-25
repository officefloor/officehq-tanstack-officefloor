import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/useClaims';
import { useOverdue } from '../overdue/overdue.slot';
import { usePeople } from '../people/people.slot';
import { useSheetDueTotal } from '../rates/rates.slot';
import { showHundredths } from '../rates/useRates';
import { STATUSES } from '../summary/summary.slot';

// The header's counters, at the top of the sheet: the people on it (retired people included), the claims in each
// status (as the summary counts them), and the sheet's due-total and overdue-total (as the rollups show them).
function SheetHeader({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const due = useSheetDueTotal(sheetId);
  const overdue = useOverdue(sheetId).sheet;
  return (
    <header data-testid="sheet-header">
      <p>
        People: <strong data-testid="header-people-count">{(people.data ?? []).length}</strong>{' '}
        {STATUSES.map((s) => (
          <span key={s}>
            {s}: <strong data-testid={`header-claims-${s}-count`}>{(claims.data ?? []).filter((c) => c.status === s).length}</strong>{' '}
          </span>
        ))}
        Due total: <strong data-testid="header-due-total">{showHundredths(due)}</strong>{' '}
        Overdue total: <strong data-testid="header-overdue-total">{showHundredths(overdue.cents)}</strong>
      </p>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -10, Component: SheetHeader });
