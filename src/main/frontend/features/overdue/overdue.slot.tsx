import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate, useTerms } from '../access/access';
import { personOverdue } from './overdue';

function OverdueHead() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue due</th>
    </>
  );
}

// A person's overdue claims that count: how many, and their due added up.
function PersonOverdue({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const terms = useTerms(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const { count, total } = personOverdue(rates, person.grade, today, terms, claims, person.held, floor, late);
  return (
    <>
      <td data-testid="person-overdue-count">{count}</td>
      <td data-testid="person-overdue-total">{total}</td>
    </>
  );
}

// The sheet's overdue claims that count: everyone's overdue count and overdue-total added up.
function OverdueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const terms = useTerms(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const each = people.map((p) =>
    personOverdue(rates, p.grade, today, terms, claims.filter((c) => c.personId === p.id), p.held, floor, late),
  );
  return (
    <p>
      Overdue: <strong data-testid="rollup-overdue-count">{each.reduce((n, o) => n + o.count, 0)}</strong>, due{' '}
      <strong data-testid="rollup-overdue-total">{sumDue(each.map((o) => o.total))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 25, Component: OverdueTotal }),
  PersonHead.fill({ order: 25, Component: OverdueHead }),
  PersonRow.fill({ order: 25, Component: PersonOverdue }),
];
