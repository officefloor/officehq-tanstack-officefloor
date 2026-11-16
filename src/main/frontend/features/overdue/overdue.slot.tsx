import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { useSheetOverdue } from '../header/totals';
import { type Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { overdue } from './overdue';

function OverdueHead() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  );
}

/** A person's overdue count and overdue-total, over their claims that count in the live totals. */
function PersonOverdue({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const mine = live(claims.data?.filter((c) => c.personId === person.id) ?? [], [person], late.today);
  const o = overdue(mine, rates.data, person, late, floor);
  return (
    <>
      <td data-testid="person-overdue-count">{o.count}</td>
      <td data-testid="person-overdue-total">{o.total}</td>
    </>
  );
}

/** The sheet's overdue count and overdue-total: everyone's added up. */
function OverdueTotal({ sheetId }: { sheetId: string }) {
  const all = useSheetOverdue(sheetId);
  return (
    <p>
      Overdue <span data-testid="rollup-overdue-count">{all.count}</span>, total{' '}
      <span data-testid="rollup-overdue-total">{all.total}</span>
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 25, Component: OverdueHead }),
  PersonRow.fill({ order: 25, Component: PersonOverdue }),
  SheetPage.fill({ order: 35, Component: OverdueTotal }),
];
