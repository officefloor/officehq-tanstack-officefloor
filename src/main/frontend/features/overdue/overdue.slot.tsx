import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useFloorCents, useTermsDays } from '../access/useAccess';
import { counts, useClaims } from '../claims/useClaims';
import { useHeld, usePeople } from '../people/people.slot';
import { claimDueCents, showHundredths, useRates } from '../rates/useRates';
import { useSheetToday } from '../today/today.slot';

/** A date (yyyy-mm-dd) plus a number of days, as yyyy-mm-dd. */
function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** A claim's due-by: its date plus the sheet's terms in force. */
function useDueBy(sheetId: string): (claim: { date: string }) => string {
  const terms = useTermsDays(sheetId);
  return (claim) => addDays(claim.date, terms);
}

/** Whether a claim is overdue: OPEN, and its due-by before the sheet's today. */
function useIsOverdue(sheetId: string): (claim: { status: string; date: string }) => boolean {
  const dueBy = useDueBy(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return (claim) => today !== undefined && claim.status === 'OPEN' && dueBy(claim) < today;
}

type Overdue = { count: number; cents: bigint };

/** The overdue claims that count, per person and for the sheet: how many, and their due added up (n/a as 0). */
function useOverdue(sheetId: string): { byPerson: Map<number, Overdue>; sheet: Overdue } {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const isOverdue = useIsOverdue(sheetId);
  const byPerson = new Map<number, Overdue>();
  const sheet: Overdue = { count: 0, cents: 0n };
  for (const p of people.data ?? []) {
    const mine: Overdue = { count: 0, cents: 0n };
    for (const c of claims.data ?? []) {
      if (c.personId !== p.id || !counts(c, held) || !isOverdue(c)) continue;
      mine.count += 1;
      mine.cents += claimDueCents(rates.data ?? [], p.grade, c, floor) ?? 0n;
    }
    byPerson.set(p.id, mine);
    sheet.count += mine.count;
    sheet.cents += mine.cents;
  }
  return { byPerson, sheet };
}

function OverdueHeads() {
  return (
    <>
      <th>Overdue</th>
      <th>Overdue total</th>
    </>
  );
}

// A person's overdue count and overdue-total: their counted OPEN claims past their due-by, and those claims' due added up.
function OverdueCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const mine = useOverdue(sheetId).byPerson.get(personId) ?? { count: 0, cents: 0n };
  return (
    <>
      <td data-testid="person-overdue-count">{mine.count}</td>
      <td data-testid="person-overdue-total">{showHundredths(mine.cents)}</td>
    </>
  );
}

// A claim's due-by (its date plus the sheet's terms) and whether it is overdue (OPEN and due-by before the sheet's today).
function ClaimOverdue({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { date: string; status: string } }) {
  const dueBy = useDueBy(sheetId);
  const isOverdue = useIsOverdue(sheetId);
  return (
    <>
      Due by <span data-testid="claim-due-by">{dueBy(claim)}</span>{' '}
      Overdue <span data-testid="claim-overdue">{isOverdue(claim) ? 'yes' : 'no'}</span>{' '}
    </>
  );
}

// The sheet's overdue count and overdue-total: everyone's added up.
function OverdueTotal({ sheetId }: { sheetId: string; user: string }) {
  const { sheet } = useOverdue(sheetId);
  return (
    <section data-testid="overdue-rollup">
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{sheet.count}</strong>{' '}
        Overdue total: <strong data-testid="rollup-overdue-total">{showHundredths(sheet.cents)}</strong>
      </p>
    </section>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 25, Component: OverdueHeads }),
  PersonCells.fill({ order: 25, Component: OverdueCells }),
  ClaimCells.fill({ order: 12, Component: ClaimOverdue }),
  SheetBody.fill({ order: 26, Component: OverdueTotal }),
];
