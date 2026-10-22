import { PersonRow } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { overdueDues, sumDue, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useClaims } from './claims';
import type { Claim } from './claims';

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
  return (
    <>
      <td data-testid="person-overdue-count">{count}</td>
      <td data-testid="person-overdue-total">{total}</td>
    </>
  );
}

/** The sheet's overdue count and overdue-total: every person's added up. */
function OverdueTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const { count, total } = useOverdue(sheetId, people, claims);
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
