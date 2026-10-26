import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts, sumAmounts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { due, rateFor, ratesQuery, sumDue } from './ratesQuery';
import type { Rate } from './ratesQuery';

/** A person's due-total: their claimed times their grade's rate, or n/a when the grade has none. */
function personDue(person: Person | undefined, claims: Claim[], rates: Rate[]): string {
  if (!person) return 'n/a';
  return due(sumAmounts(claims.filter((c) => c.personId === person.id && counts(c))), rateFor(rates, person.grade));
}

function useSheetData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
  };
}

function ClaimDue({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { people, rates } = useSheetData(sheetId);
  const person = people.find((p) => p.id === claim.personId);
  return <td data-testid="claim-due">{due(claim.amount, rateFor(rates, person?.grade))}</td>;
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates } = useSheetData(sheetId);
  return <td data-testid="person-due-total">{personDue(people.find((p) => p.id === personId), claims, rates)}</td>;
}

function DueTotal({ sheetId }: { sheetId: string }) {
  const { people, claims, rates } = useSheetData(sheetId);
  return (
    <p data-testid="rollup-due">
      Due total <strong data-testid="rollup-due-total">{sumDue(people.map((p) => personDue(p, claims, rates)))}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  PersonHeaders.fill({ order: 15, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 15, Component: PersonDueTotal }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
];
