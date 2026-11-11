import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { trimmedSumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { personOverdue } from './overdue';
import { Col } from '../columns/columns';

function OverdueHead() {
  return (
    <>
      <Col name="overdue-count"><th>Overdue</th></Col>
      <Col name="overdue-total"><th>Overdue due</th></Col>
    </>
  );
}

// A person's overdue claims that count: how many, and their due added up.
function PersonOverdue({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const { count, total } = personOverdue(rates, person.grades, today, claims, liveHolds(person, today), floor, late);
  return (
    <>
      <Col name="overdue-count"><td data-testid="person-overdue-count">{count}</td></Col>
      <Col name="overdue-total"><td data-testid="person-overdue-total">{total}</td></Col>
    </>
  );
}

// The sheet's overdue claims that count: everyone's overdue count and overdue-total added up
// (the overdue-total trimmed of the highest and lowest person's).
function OverdueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const each = people.map((p) =>
    personOverdue(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late),
  );
  return (
    <p>
      Overdue: <strong data-testid="rollup-overdue-count">{each.reduce((n, o) => n + o.count, 0)}</strong>, due{' '}
      <strong data-testid="rollup-overdue-total">{trimmedSumDue(each.map((o) => o.total))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 25, Component: OverdueTotal }),
  PersonHead.fill({ order: 25, Component: OverdueHead }),
  PersonRow.fill({ order: 25, Component: PersonOverdue }),
];
