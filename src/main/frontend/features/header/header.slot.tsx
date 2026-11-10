import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { personDueTotal, trimmedSumDue, useRates } from '../rates/rates';
import { personOverdue } from '../overdue/overdue';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';

// Every status the header counts — the same as the summary's.
const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

// The sheet's header counters, at the top of the sheet: everyone on it (retired people included),
// the claims in each status, and the sheet's due-total and overdue-total — worked from the same
// figures as the summary and the sheet's totals, so each always equals the figure it repeats.
function Header({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);

  const claimsOf = (id: string) => claims.filter((c) => c.personId === id);
  const dueTotal = trimmedSumDue(
    people.map((p) => personDueTotal(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late, cap)),
  );
  const overdueTotal = trimmedSumDue(
    people.map((p) => personOverdue(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late).total),
  );

  return (
    <header data-testid="header">
      <span>
        People: <strong data-testid="header-people-count">{people.length}</strong>
      </span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}
          {s}: <strong data-testid={`header-claims-${s}-count`}>{claims.filter((c) => c.status === s).length}</strong>
        </span>
      ))}{' '}
      <span>
        Due: <strong data-testid="header-due-total">{dueTotal}</strong>
      </span>{' '}
      <span>
        Overdue: <strong data-testid="header-overdue-total">{overdueTotal}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -100, Component: Header });
