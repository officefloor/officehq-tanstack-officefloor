import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useFloor, useLate } from '../settings/settingsQuery';
import { figure, isOverdue, ratesQuery, score, settledAndOpen, sumDue, trimmedSum } from './ratesQuery';
import { useColumnsShown } from '../people/useColumns';

function useOverdueData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    floor: useFloor(sheetId),
    late: useLate(sheetId),
  };
}

type OverdueData = ReturnType<typeof useOverdueData>;

/** The overdue claims that count (a retired person's unbound open claims do not), of one person or of the sheet, with their figures, late charges included. */
function overdue({ people, claims, rates, today, floor, late }: OverdueData, person?: Person) {
  const overdueClaims = claims.filter(
    (c) => (!person || c.personId === person.id) && counts(c, people, today) && isOverdue(c, late),
  );
  const figures = overdueClaims.map((c) => figure(c, rates, people.find((p) => p.id === c.personId), floor, late));
  return { count: overdueClaims.length, total: sumDue(figures) };
}

function ClaimDueBy({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const late = useLate(sheetId);
  return (
    <>
      <td data-testid="claim-due-by">{claim.dueBy}</td>
      <td data-testid="claim-overdue">{isOverdue(claim, late) ? 'yes' : 'no'}</td>
    </>
  );
}

function OverdueHeader() {
  const countShown = useColumnsShown('overdue-count');
  const totalShown = useColumnsShown('overdue-total');
  return countShown || totalShown ? <th>Overdue</th> : null;
}

function PersonOverdue({ sheetId, personId }: { sheetId: string; personId: number }) {
  const data = useOverdueData(sheetId);
  const person = data.people.find((p) => p.id === personId);
  const { count, total } = person ? overdue(data, person) : { count: 0, total: '0.00' };
  const countShown = useColumnsShown('overdue-count');
  const totalShown = useColumnsShown('overdue-total');
  // The cell holds two columns: it goes only when both are switched off.
  if (!countShown && !totalShown) return null;
  return (
    <td>
      {countShown && <span data-testid="person-overdue-count">{count}</span>}
      {countShown && totalShown && ' / '}
      {totalShown && <span data-testid="person-overdue-total">{total}</span>}
    </td>
  );
}

/** A person's score: their settled-total / 100, minus 5 per overdue claim of theirs that counts, rounded half to even. */
function PersonScore({ sheetId, personId }: { sheetId: string; personId: number }) {
  const data = useOverdueData(sheetId);
  const person = data.people.find((p) => p.id === personId);
  const { count } = person ? overdue(data, person) : { count: 0 };
  const [settled] = settledAndOpen(person, data.people, data.claims, data.rates, data.today, data.floor, data.late);
  return <td data-testid="person-score">{score(settled, count)}</td>;
}

/** The sheet's overdue claims that count: how many, and their total (every person's, trimmed of the highest and lowest). */
export function useSheetOverdue(sheetId: string) {
  const data = useOverdueData(sheetId);
  const { count } = overdue(data);
  return { count, total: trimmedSum(data.people.map((p) => overdue(data, p).total)) };
}

/** Every person's overdue-total, as in their row, in the order of the sheet's people. */
export function usePeopleOverdueTotals(sheetId: string): string[] {
  const data = useOverdueData(sheetId);
  return data.people.map((p) => overdue(data, p).total);
}

/** One person's overdue-total, as in their row; null when there is no such person. */
export function usePersonOverdueTotal(sheetId: string, personId: number | undefined) {
  const data = useOverdueData(sheetId);
  const person = data.people.find((p) => p.id === personId);
  return person ? overdue(data, person).total : null;
}

function OverdueTotal({ sheetId }: { sheetId: string }) {
  const { count, total } = useSheetOverdue(sheetId);
  return (
    <p data-testid="rollup-overdue">
      Overdue <strong data-testid="rollup-overdue-count">{count}</strong> totalling{' '}
      <strong data-testid="rollup-overdue-total">{total}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 12, Component: ClaimDueBy }),
  PersonHeaders.fill({ order: 16, Component: OverdueHeader }),
  PersonCells.fill({ order: 16, Component: PersonOverdue }),
  PersonHeaders.fill({ order: 16.5, Component: () => <th>Score</th> }),
  PersonCells.fill({ order: 16.5, Component: PersonScore }),
  SheetPanel.fill({ order: 26, Component: OverdueTotal }),
];
