import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import type { Person } from '../people/people';
import { hundredths, twoDecimals } from '../rates/rates';
import { usePools, type Pool } from './pools';

/** A person's cut total in hundredths: their cuts across every pool they are a member of, a pool that shows no cut
 *  adding nothing. */
function personCutCents(pools: Pool[], personId: number): bigint {
  let cents = 0n;
  for (const pool of pools) {
    for (const m of pool.members) {
      if (m.memberPool === null && m.person === personId && m.cut !== '') cents += hundredths(m.cut);
    }
  }
  return cents;
}

/** The sheet's cut total in hundredths: every person's cut total added up. */
function sheetCutCents(pools: Pool[]): bigint {
  let cents = 0n;
  for (const pool of pools) {
    for (const m of pool.members) {
      if (m.memberPool === null && m.cut !== '') cents += hundredths(m.cut);
    }
  }
  return cents;
}

function CutTotalHeader() {
  return <th>Cut total</th>;
}

function PersonCutTotal({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: pools = [] } = usePools(sheet);
  return <td data-testid="person-cut-total">{twoDecimals(personCutCents(pools, person.id))}</td>;
}

function SheetCutTotal({ sheet }: { sheet: string; user: string }) {
  const { data: pools = [] } = usePools(sheet);
  return (
    <p>
      Cut total: <strong data-testid="rollup-cut-total">{twoDecimals(sheetCutCents(pools))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 9.8, Component: CutTotalHeader }),
  PersonColumns.fill({ order: 9.8, Component: PersonCutTotal }),
  SheetPanels.fill({ order: 20.1, Component: SheetCutTotal }),
];
