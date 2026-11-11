import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { personOverdue } from '../overdue/overdue';
import { useUser } from '../session/session';
import { useAsOf } from '../sheet/asof.slot';
import { useDescendantFigures } from '../consolidated/consolidated';
import { useMyRole } from './dashboard';

// The steward's dashboard: over the sheet and all its descendants, how many claims are PENDING, how many are in REVIEW, and
// how many overdue claims count (on each sheet the same figure as its overdue count). Shown only to a user
// acting as a steward — the cells are empty for anyone else; as of a date, by the role they had then.
function Dashboard({ sheetId }: { sheetId: string }) {
  const [user] = useUser();
  const role = useMyRole(sheetId, user).data;
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const [asOf] = useAsOf();
  const descendants = useDescendantFigures(sheetId, asOf).data ?? [];

  const steward = role === 'steward';
  const overdue = people
    .map((p) => personOverdue(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late))
    .reduce((n, o) => n + o.count, descendants.reduce((n, f) => n + f.overdueCount, 0));
  const status = (s: string) =>
    claims.filter((c) => c.status === s).length + descendants.reduce((n, f) => n + f.statusCounts[s], 0);
  const shown = (n: number) => (steward ? String(n) : '');

  return (
    <section data-testid="dashboard">
      Steward: PENDING <strong data-testid="dash-pending-count">{shown(status('PENDING'))}</strong>,
      REVIEW <strong data-testid="dash-review-count">{shown(status('REVIEW'))}</strong>,
      overdue <strong data-testid="dash-overdue-count">{shown(overdue)}</strong>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -98, Component: Dashboard });
