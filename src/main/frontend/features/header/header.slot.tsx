import { SheetBody } from '../../slots/defs/sheet';
import { STATUSES, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { overdueDues, sheetDueTotal, sumDue, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** The sheet's header counters, shown above everything else: its people (retired ones included), its claims
 *  per status (as the summary counts them) and its due-total and overdue-total (as the rollup shows them). */
function Header({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  return (
    <header data-testid="header">
      <span>
        People <strong data-testid="header-people-count">{people.length}</strong>
      </span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s} <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}{' '}
      <span>
        Due total <strong data-testid="header-due-total">{sheetDueTotal(people, claims, rates, today, floor, late)}</strong>
      </span>{' '}
      <span>
        Overdue total <strong data-testid="header-overdue-total">{sumDue(overdueDues(claims, people, rates, late, floor))}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -1, Component: Header });
