import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { useFloor } from '../settings/settingsQuery';
import type { Person } from '../people/peopleQuery';
import { figure, rateFor, ratesQuery, sumDue } from './ratesQuery';
import type { Rate } from './ratesQuery';

/**
 * A person's due-total: the sum of their counting claims' rounded figures, as each claim shows it. With no rate today and no figure it is n/a; otherwise it is the sum of the figures there are.
 */
function personDue(person: Person | undefined, people: Person[], claims: Claim[], rates: Rate[], today: string, floor: string): string {
  if (!person) return 'n/a';
  const rate = rateFor(rates, person.grade, today);
  const figures = claims
    .filter((c) => c.personId === person.id && counts(c, people))
    .map((c) => figure(c, rates, person.grade, floor))
    .filter((d) => d !== 'n/a');
  return rate === undefined && figures.length === 0 ? 'n/a' : sumDue(figures);
}

function useSheetData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
    floor: useFloor(sheetId),
  };
}

function ClaimDue({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { people, rates, floor } = useSheetData(sheetId);
  const person = people.find((p) => p.id === claim.personId);
  return <td data-testid="claim-due">{figure(claim, rates, person?.grade, floor)}</td>;
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today, floor } = useSheetData(sheetId);
  return <td data-testid="person-due-total">{personDue(people.find((p) => p.id === personId), people, claims, rates, today, floor)}</td>;
}

function DueTotal({ sheetId }: { sheetId: string }) {
  const { people, claims, rates, today, floor } = useSheetData(sheetId);
  return (
    <p data-testid="rollup-due">
      Due total <strong data-testid="rollup-due-total">{sumDue(people.map((p) => personDue(p, people, claims, rates, today, floor)))}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  PersonHeaders.fill({ order: 15, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 15, Component: PersonDueTotal }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
];
