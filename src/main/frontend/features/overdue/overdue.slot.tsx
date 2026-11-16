import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { sum, useRates } from '../rates/rates';
import { useSheetToday } from '../session/session';
import { useFloor, useTerms } from '../settings/settings';
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
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const terms = useTerms(sheetId);
  const mine = live(claims.data?.filter((c) => c.personId === person.id) ?? [], [person]);
  const o = overdue(mine, rates.data, person.grade, terms, today, floor);
  return (
    <>
      <td data-testid="person-overdue-count">{o.count}</td>
      <td data-testid="person-overdue-total">{o.total}</td>
    </>
  );
}

/** The sheet's overdue count and overdue-total: everyone's added up. */
function OverdueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const terms = useTerms(sheetId);
  const all = (people.data ?? []).map((p) =>
    overdue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p]), rates.data, p.grade, terms, today, floor),
  );
  return (
    <p>
      Overdue <span data-testid="rollup-overdue-count">{all.reduce((n, o) => n + o.count, 0)}</span>, total{' '}
      <span data-testid="rollup-overdue-total">{sum(all.map((o) => o.total))}</span>
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 25, Component: OverdueHead }),
  PersonRow.fill({ order: 25, Component: PersonOverdue }),
  SheetPage.fill({ order: 35, Component: OverdueTotal }),
];
