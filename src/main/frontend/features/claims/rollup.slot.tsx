import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/usePeople';
import { dueCents, rateByGrade, totalDue, useRates } from '../rates/useRates';
import { claimed, counted, useClaims } from './useClaims';

/**
 * The sheet's claimed-total, everyone's counted (OPEN or SETTLED) claims added up, and its due-total, everyone's due-total
 * added up (a person whose grade has no rate is skipped).
 */
function ClaimedTotal({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const claims = counted(all);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const pct = rateByGrade(rates);
  const dues = people.map((p) =>
    dueCents(claimed(claims.filter((c) => c.personId === p.id)), pct.get(p.grade)),
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
