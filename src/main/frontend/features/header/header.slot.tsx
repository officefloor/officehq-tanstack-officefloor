import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { twoDecimals } from '../rates/rates';
import { CLAIM_STATUSES, statusCount, useClaims, useSheetTotals } from '../claims/claims';

// The sheet's counters, shown at all times above everything else. Each repeats a figure figured elsewhere, from the
// same helpers: the people on the sheet (retired ones included), the claims per status (as the summary counts them),
// and the sheet's due-total and overdue-total (as the rollup shows them).
function Header({ sheet }: { sheet: string; user: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { due, overdue } = useSheetTotals(sheet);
  return (
    <header>
      <span>
        People: <strong data-testid="header-people-count">{people.length}</strong>
      </span>
      {CLAIM_STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims, s)}</strong>
        </span>
      ))}
      <span>
        {' '}
        Due total: <strong data-testid="header-due-total">{twoDecimals(due)}</strong>
      </span>
      <span>
        {' '}
        Overdue total: <strong data-testid="header-overdue-total">{twoDecimals(overdue.cents)}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetPanels.fill({ order: -10, Component: Header });
