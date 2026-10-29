import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { GRADES, gradeOn, peopleQuery } from '../people/peopleQuery';
import { patronSharesOf, patronsQuery } from '../patrons/patronsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useCap, useFloor, useLate } from '../settings/settingsQuery';
import { agedDue, excess, figure, personDue, personSum, ratesQuery, recentSettled, settledAndOpen, sumDue, sumOfShares, trimmedSum } from './ratesQuery';
import { ColumnShown } from '../people/useColumns';

function useSheetData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    floor: useFloor(sheetId),
    late: useLate(sheetId),
    cap: useCap(sheetId),
  };
}

function ClaimDue({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { people, rates, floor, late } = useSheetData(sheetId);
  const person = people.find((p) => p.id === claim.personId);
  return <td data-testid="claim-due">{figure(claim, rates, person, floor, late)}</td>;
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  return <td data-testid="person-due-total">{personDue(people.find((p) => p.id === personId), people, claims, rates, today, floor, late, cap)}</td>;
}

/** What a person's figures add up to above the sheet's cap; 0.00 when they are not above it. */
function PersonExcess({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  return <td data-testid="person-excess">{excess(personSum(people.find((p) => p.id === personId), people, claims, rates, today, floor, late), cap)}</td>;
}

/** A person's open claims' figures that count, split by the claim's age on the sheet's today: 0-30, 31-60 and 61+ days. */
function PersonAged({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  const [young, middle, old] = agedDue(people.find((p) => p.id === personId), people, claims, rates, today, floor, late);
  return (
    <>
      <td data-testid="person-aged-0-30">{young}</td>
      <td data-testid="person-aged-31-60">{middle}</td>
      <td data-testid="person-aged-61">{old}</td>
    </>
  );
}

/**
 * A person's backing: each patron's recent settled figure (see `recentSettled`) times their share (see `patronSharesOf`)
 * percent, added up and rounded half to even; 0.00 with no patrons.
 */
function PersonBacking({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, today } = useSheetData(sheetId);
  const shares = patronSharesOf(useQuery(patronsQuery(sheetId)).data ?? [], personId);
  const parts = shares
    .filter((s) => people.some((p) => p.id === s.patronId))
    .map((s): [string, string] => [recentSettled(s.patronId, claims, today), s.share]);
  return <td data-testid="person-backing">{sumOfShares(parts)}</td>;
}

/** A person's settled-total and open-total: their counting SETTLED, and OPEN and PENDING, claims' figures added up (not capped). */
function PersonSettledOpen({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  const [settled, open] = settledAndOpen(people.find((p) => p.id === personId), people, claims, rates, today, floor, late);
  return (
    <>
      <td data-testid="person-settled-total">{settled}</td>
      <td data-testid="person-open-total">{open}</td>
    </>
  );
}

/** The sheet's settled-total and open-total: everyone's added up (not trimmed). */
function SettledOpenTotal({ sheetId }: { sheetId: string }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  const split = people.map((p) => settledAndOpen(p, people, claims, rates, today, floor, late));
  return (
    <p data-testid="rollup-settled-open">
      Settled <strong data-testid="rollup-settled-total">{sumDue(split.map(([s]) => s))}</strong>
      {' '}· Open <strong data-testid="rollup-open-total">{sumDue(split.map(([, o]) => o))}</strong>
    </p>
  );
}

/** The sheet's age buckets: everyone's open claims' figures by age (0-30, 31-60, 61+ days) added up (not trimmed). */
function AgedTotal({ sheetId }: { sheetId: string }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  const aged = people.map((p) => agedDue(p, people, claims, rates, today, floor, late));
  return (
    <p data-testid="rollup-aged">
      0–30 <strong data-testid="rollup-aged-0-30">{sumDue(aged.map(([y]) => y))}</strong>
      {' '}· 31–60 <strong data-testid="rollup-aged-31-60">{sumDue(aged.map(([, m]) => m))}</strong>
      {' '}· 61+ <strong data-testid="rollup-aged-61">{sumDue(aged.map(([, , o]) => o))}</strong>
    </p>
  );
}

/** The sheet's due-total: every person's due-total added up, skipping n/a, trimmed of the highest and lowest. */
export function useSheetDueTotal(sheetId: string) {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  return trimmedSum(people.map((p) => personDue(p, people, claims, rates, today, floor, late, cap)));
}

/**
 * Per grade (as `GRADES` orders them), the due-totals of the people on it today, retired people left out, added up
 * skipping n/a — as the summary shows them.
 */
export function useGradeDueTotals(sheetId: string): string[] {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  const unretired = people.filter((p) => p.retiredFrom === null);
  return GRADES.map((g) =>
    sumDue(unretired.filter((p) => gradeOn(p, today) === g).map((p) => personDue(p, people, claims, rates, today, floor, late, cap))),
  );
}

/** One person's due-total, as in their row; null when there is no such person. */
export function usePersonDueTotal(sheetId: string, personId: number | undefined) {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  const person = people.find((p) => p.id === personId);
  return person ? personDue(person, people, claims, rates, today, floor, late, cap) : null;
}

/** Every person's due-total, as in their row, by person id. */
export function usePeopleDueTotals(sheetId: string): Map<number, string> {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  return new Map(people.map((p) => [p.id, personDue(p, people, claims, rates, today, floor, late, cap)]));
}

function DueTotal({ sheetId }: { sheetId: string }) {
  return (
    <p data-testid="rollup-due">
      Due total <strong data-testid="rollup-due-total">{useSheetDueTotal(sheetId)}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  PersonHeaders.fill({ order: 15, Component: () => <ColumnShown column="due-total"><th>Due</th></ColumnShown> }),
  PersonCells.fill({ order: 15, Component: (ctx) => <ColumnShown column="due-total"><PersonDueTotal {...ctx} /></ColumnShown> }),
  PersonHeaders.fill({ order: 15.2, Component: () => <><th>Settled</th><th>Open</th></> }),
  PersonCells.fill({ order: 15.2, Component: PersonSettledOpen }),
  PersonHeaders.fill({ order: 15.5, Component: () => <><th>0–30</th><th>31–60</th><th>61+</th></> }),
  PersonCells.fill({ order: 15.5, Component: PersonAged }),
  PersonHeaders.fill({ order: 16, Component: () => <ColumnShown column="excess"><th>Excess</th></ColumnShown> }),
  PersonCells.fill({ order: 16, Component: (ctx) => <ColumnShown column="excess"><PersonExcess {...ctx} /></ColumnShown> }),
  PersonHeaders.fill({ order: 22, Component: () => <ColumnShown column="backing"><th>Backing</th></ColumnShown> }),
  PersonCells.fill({ order: 22, Component: (ctx) => <ColumnShown column="backing"><PersonBacking {...ctx} /></ColumnShown> }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
  SheetPanel.fill({ order: 25.5, Component: SettledOpenTotal }),
  SheetPanel.fill({ order: 25.55, Component: AgedTotal }),
];
