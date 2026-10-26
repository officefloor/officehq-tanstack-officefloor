import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useFloorCents, useLate } from '../access/useAccess';
import { counts, useClaims } from '../claims/useClaims';
import { gradeOn, useHeld, usePeople } from '../people/people.slot';
import { claimDueCents, isOpen, lateFrom, showHundredths, trimmedSum, useRates } from '../rates/useRates';
import { useSheetToday } from '../today/today.slot';
import { useColumnShown } from '../columns/useColumns';

/** Whether a claim is overdue: OPEN or PENDING, not deferred past the sheet's today, and its due-by plus its grade's
 *  grace (see lateFrom; `grade` is its person's grade on its date) before the sheet's today. */
function useIsOverdue(sheetId: string): (claim: { status: string; date: string; dueBy: string; deferred?: boolean }, grade: string | undefined) => boolean {
  const today = useSheetToday(sheetId).data?.today;
  const late = useLate(sheetId);
  return (claim, grade) => today !== undefined && isOpen(claim) && !claim.deferred && lateFrom(late, grade, claim) < today;
}

type Overdue = { count: number; cents: bigint };

/** The overdue claims that count, per person and for the sheet: how many, and their due (late charge included) added up (n/a as 0);
 *  the sheet's total leaves out the single highest and the single lowest person's when there are three or more people. */
export function useOverdue(sheetId: string): { byPerson: Map<number, Overdue>; sheet: Overdue } {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const isOverdue = useIsOverdue(sheetId);
  const byPerson = new Map<number, Overdue>();
  const sheet: Overdue = { count: 0, cents: 0n };
  for (const p of people.data ?? []) {
    const mine: Overdue = { count: 0, cents: 0n };
    for (const c of claims.data ?? []) {
      if (c.personId !== p.id || !counts(c, held) || !isOverdue(c, gradeOn(p, c.date))) continue;
      mine.count += 1;
      mine.cents += claimDueCents(rates.data ?? [], gradeOn(p, c.date), c, floor, late) ?? 0n;
    }
    byPerson.set(p.id, mine);
    sheet.count += mine.count;
  }
  sheet.cents = trimmedSum([...byPerson.values()].map((o) => o.cents));
  return { byPerson, sheet };
}

function OverdueHeads({ sheetId }: { sheetId: string; user: string }) {
  const shown = useColumnShown(sheetId);
  return (
    <>
      {shown('overdue-count') && <th>Overdue</th>}
      {shown('overdue-total') && <th>Overdue total</th>}
    </>
  );
}

// A person's overdue count and overdue-total: their counted OPEN claims past their due-by, and those claims' due added up.
function OverdueCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const shown = useColumnShown(sheetId);
  const mine = useOverdue(sheetId).byPerson.get(personId) ?? { count: 0, cents: 0n };
  return (
    <>
      {shown('overdue-count') && <td data-testid="person-overdue-count">{mine.count}</td>}
      {shown('overdue-total') && <td data-testid="person-overdue-total">{showHundredths(mine.cents)}</td>}
    </>
  );
}

// A claim's due-by (the terms-th business day after its date, by the terms fixed when it was added and the sheet's holidays now) and whether it is overdue (OPEN and due-by plus its grade's grace before the sheet's today).
function ClaimOverdue({ sheetId, personId, claim }: { sheetId: string; user: string; personId: number; claim: { status: string; date: string; dueBy: string; deferred?: boolean } }) {
  const isOverdue = useIsOverdue(sheetId);
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  return (
    <>
      Due by <span data-testid="claim-due-by">{claim.dueBy}</span>{' '}
      Overdue <span data-testid="claim-overdue">{isOverdue(claim, gradeOn(person, claim.date)) ? 'yes' : 'no'}</span>{' '}
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
