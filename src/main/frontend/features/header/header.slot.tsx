import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import type { ClaimStatus } from '../claims/claims';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { sheetFigures } from '../consolidated/consolidated';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { useUser } from '../session/session';

// Names are compared trimmed and ignoring case, aliases alike.
const asName = (s: string) => s.trim().toLowerCase();

// Every status the header counts — the same as the summary's.
const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

// The sheet's header counters, at the top of the sheet: everyone on it (retired people included),
// the claims in each status, and the sheet's due-total and overdue-total — worked from the same
// figures as the summary and the sheet's totals, so each always equals the figure it repeats.
// Beside them, the signed-in user's own figures: the due-total and overdue-total of the person whose
// name or alias is the user's name — blank when there is no such person.
function Header({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const [user] = useUser();

  const { dueTotal, overdueTotal } = sheetFigures(people, claims, rates, today, floor, late, cap);
  const me = user ? people.find((p) => [p.name, ...p.aliases].some((n) => asName(n) === asName(user))) : undefined;
  const mine = me ? sheetFigures([me], claims, rates, today, floor, late, cap) : undefined;

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
      </span>{' '}
      <span>
        My due: <strong data-testid="my-due-total">{mine?.dueTotal ?? ''}</strong>
      </span>{' '}
      <span>
        My overdue: <strong data-testid="my-overdue-total">{mine?.overdueTotal ?? ''}</strong>
      </span>
    </header>
  );
}

export const contribution = SheetBody.fill({ order: -100, Component: Header });
