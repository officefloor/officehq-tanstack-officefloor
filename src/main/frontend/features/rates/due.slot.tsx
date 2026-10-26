import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { patronIdsOf, patronsQuery } from '../patrons/patronsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useCap, useFloor, useLate } from '../settings/settingsQuery';
import { agedDue, excess, figure, personDue, personSum, ratesQuery, recentSettled, sumDue, trimmedSum } from './ratesQuery';

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

/** A person's backing: their patrons' recent settled figures (see `recentSettled`) added up; 0.00 with no patrons. */
function PersonBacking({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, today } = useSheetData(sheetId);
  const ids = patronIdsOf(useQuery(patronsQuery(sheetId)).data ?? [], personId);
  const sums = people.filter((p) => ids.includes(p.id)).map((p) => recentSettled(p.id, claims, today));
  return <td data-testid="person-backing">{sumDue(sums)}</td>;
}

/** The sheet's due-total: every person's due-total added up, skipping n/a, trimmed of the highest and lowest. */
export function useSheetDueTotal(sheetId: string) {
  const { people, claims, rates, today, floor, late, cap } = useSheetData(sheetId);
  return trimmedSum(people.map((p) => personDue(p, people, claims, rates, today, floor, late, cap)));
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
  PersonHeaders.fill({ order: 15.5, Component: () => <><th>0–30</th><th>31–60</th><th>61+</th></> }),
  PersonCells.fill({ order: 15.5, Component: PersonAged }),
  PersonHeaders.fill({ order: 16, Component: () => <th>Excess</th> }),
  PersonCells.fill({ order: 16, Component: PersonExcess }),
  PersonHeaders.fill({ order: 22, Component: () => <th>Backing</th> }),
  PersonCells.fill({ order: 22, Component: PersonBacking }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
];
