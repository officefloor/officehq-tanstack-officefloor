import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { overdueDues, sheetOverdueTotal, sumDue, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useClaims } from './claims';
import type { Claim } from './claims';
import { useColumnShown } from '../people/columns';

function useOverdue(sheetId: string, people: Person[], claims: Claim[]) {
  const rates = useRates(sheetId).data ?? [];
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const dues = overdueDues(claims, people, rates, late, floor);
  return { count: dues.length, total: sumDue(dues) };
}

/** A person's overdue count and overdue-total: the due of their overdue claims that count
 *  (late charges included) added up. */
function PersonOverdue({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const { count, total } = useOverdue(sheetId, [person], claims);
  const showCount = useColumnShown('overdue-count');
  const showTotal = useColumnShown('overdue-total');
  return (
    <>
      {showCount && <td data-testid="person-overdue-count">{count}</td>}
      {showTotal && <td data-testid="person-overdue-total">{total}</td>}
    </>
  );
}

/** The sheet's overdue count (every person's added up) and overdue-total (trimmed, see sheetOverdueTotal). */
function OverdueTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const { count } = useOverdue(sheetId, people, claims);
  const total = sheetOverdueTotal(people, claims, rates, late, floor);
  return (
    <p>
      Overdue <strong data-testid="rollup-overdue-count">{count}</strong> totalling{' '}
      <strong data-testid="rollup-overdue-total">{total}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 22, Component: OverdueTotal }),
  PersonRow.fill({ order: 25, Component: PersonOverdue }),
];
