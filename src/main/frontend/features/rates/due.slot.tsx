import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { patronIdsOf, patronsQuery } from '../patrons/patronsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useFloor, useLate } from '../settings/settingsQuery';
import { figure, personDue, ratesQuery, sumDue } from './ratesQuery';

function useSheetData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    floor: useFloor(sheetId),
    late: useLate(sheetId),
  };
}

function ClaimDue({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { people, rates, floor, late } = useSheetData(sheetId);
  const person = people.find((p) => p.id === claim.personId);
  return <td data-testid="claim-due">{figure(claim, rates, person, floor, late)}</td>;
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  return <td data-testid="person-due-total">{personDue(people.find((p) => p.id === personId), people, claims, rates, today, floor, late)}</td>;
}

/** A person's backing: their patrons' due-totals added up, skipping n/a; 0.00 with no patrons. */
function PersonBacking({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  const ids = patronIdsOf(useQuery(patronsQuery(sheetId)).data ?? [], personId);
  const dues = people.filter((p) => ids.includes(p.id)).map((p) => personDue(p, people, claims, rates, today, floor, late));
  return <td data-testid="person-backing">{sumDue(dues)}</td>;
}

/** The sheet's due-total: every person's due-total added up, skipping n/a. */
export function useSheetDueTotal(sheetId: string) {
  const { people, claims, rates, today, floor, late } = useSheetData(sheetId);
  return sumDue(people.map((p) => personDue(p, people, claims, rates, today, floor, late)));
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
  PersonHeaders.fill({ order: 15, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 15, Component: PersonDueTotal }),
  PersonHeaders.fill({ order: 22, Component: () => <th>Backing</th> }),
  PersonCells.fill({ order: 22, Component: PersonBacking }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
];
