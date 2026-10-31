import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/usePeople';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { claimed, live, personDueCents, useClaims } from './useClaims';

/**
 * The sheet's claimed-total, everyone's counted (OPEN or SETTLED) claims added up
 * (not a held person's OPEN ones), and its due-total, everyone's due-total
 * added up (a person whose due-total is n/a is skipped).
 */
function ClaimedTotal({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  const claims = live(all, (id) => held.has(id));
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const dues = people.map((p) =>
    personDueCents(claims.filter((c) => c.personId === p.id), floor, rateOn(rates, p.grade, today), (d) => rateOn(rates, p.grade, d)),
  );
  return (
    <>
      <p>
        Claimed in total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
      </p>
      <p>
        Due in total: <strong data-testid="rollup-due-total">{totalDue(dues)}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
