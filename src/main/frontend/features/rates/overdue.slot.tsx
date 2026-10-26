import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useFloor, useLate, useTerms } from '../settings/settingsQuery';
import { dueBy, figure, isOverdue, ratesQuery, sumDue } from './ratesQuery';

function useOverdueData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    floor: useFloor(sheetId),
    terms: useTerms(sheetId),
    late: useLate(sheetId),
  };
}

type OverdueData = ReturnType<typeof useOverdueData>;

/** The overdue claims that count (a held person's OPEN claims do not), of one person or of the sheet, with their figures, late charges included. */
function overdue({ people, claims, rates, today, floor, terms, late }: OverdueData, person?: Person) {
  const overdueClaims = claims.filter(
    (c) => (!person || c.personId === person.id) && counts(c, people) && isOverdue(c, terms, today),
  );
  const figures = overdueClaims.map((c) => figure(c, rates, people.find((p) => p.id === c.personId)?.grade, floor, late));
  return { count: overdueClaims.length, total: sumDue(figures) };
}

function ClaimDueBy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { today, terms } = useOverdueData(sheetId);
  return (
    <>
      <td data-testid="claim-due-by">{dueBy(claim, terms)}</td>
      <td data-testid="claim-overdue">{isOverdue(claim, terms, today) ? 'yes' : 'no'}</td>
    </>
  );
}

function PersonOverdue({ sheetId, personId }: { sheetId: string; personId: number }) {
  const data = useOverdueData(sheetId);
  const person = data.people.find((p) => p.id === personId);
  const { count, total } = person ? overdue(data, person) : { count: 0, total: '0.00' };
  return (
    <td>
      <span data-testid="person-overdue-count">{count}</span> /{' '}
      <span data-testid="person-overdue-total">{total}</span>
    </td>
  );
}

function OverdueTotal({ sheetId }: { sheetId: string }) {
  const { count, total } = overdue(useOverdueData(sheetId));
  return (
    <p data-testid="rollup-overdue">
      Overdue <strong data-testid="rollup-overdue-count">{count}</strong> totalling{' '}
      <strong data-testid="rollup-overdue-total">{total}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 12, Component: ClaimDueBy }),
  PersonHeaders.fill({ order: 16, Component: () => <th>Overdue</th> }),
  PersonCells.fill({ order: 16, Component: PersonOverdue }),
  SheetPanel.fill({ order: 26, Component: OverdueTotal }),
];
