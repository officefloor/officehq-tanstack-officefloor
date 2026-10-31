import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradeOn, usePeople } from '../people/usePeople';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { claimed, live, overdueFigures, personDueCents, useClaims } from './useClaims';

/**
 * The sheet's claimed-total, everyone's counted (OPEN or SETTLED) claims added up
 * (not a held person's OPEN ones), and its due-total, everyone's due-total
 * added up (a person whose due-total is n/a is skipped), and how many of the claims that count are
 * overdue, with their dues added up.
 */
function ClaimedTotal({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  const claims = live(all, (id) => held.has(id));
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const dues = people.map((p) =>
    personDueCents(claims.filter((c) => c.personId === p.id), floor, rateOn(rates, gradeOn(p, today), today), (d) => rateOn(rates, gradeOn(p, d), d), late),
  );
  const over = people.map((p) =>
    overdueFigures(claims.filter((c) => c.personId === p.id), late, floor, (d) => rateOn(rates, gradeOn(p, d), d)),
  );
  return (
    <>
      <p>
        Claimed in total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
      </p>
      <p>
        Due in total: <strong data-testid="rollup-due-total">{totalDue(dues)}</strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{over.reduce((n, o) => n + o.count, 0)}</strong>, due{' '}
        <strong data-testid="rollup-overdue-total">{totalDue(over.map((o) => o.cents))}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
