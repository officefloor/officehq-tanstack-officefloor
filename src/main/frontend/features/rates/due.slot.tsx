import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts, sumAmounts } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import type { Person } from '../people/peopleQuery';
import { due, rateFor, ratesQuery, sumDue } from './ratesQuery';
import type { Rate } from './ratesQuery';

/** A person's due-total: their claimed times their grade's rate in force on the sheet's today, or n/a when none is. */
function personDue(person: Person | undefined, claims: Claim[], rates: Rate[], today: string): string {
  if (!person) return 'n/a';
  return due(sumAmounts(claims.filter((c) => c.personId === person.id && counts(c))), rateFor(rates, person.grade, today));
}

function useSheetData(sheetId: string) {
  return {
    people: useQuery(peopleQuery(sheetId)).data ?? [],
    claims: useQuery(claimsQuery(sheetId)).data ?? [],
    rates: useQuery(ratesQuery(sheetId)).data ?? [],
    today: useSheetToday(sheetId),
  };
}

function ClaimDue({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const { people, rates } = useSheetData(sheetId);
  const person = people.find((p) => p.id === claim.personId);
  // A settled claim shows the due it was fixed at; otherwise its due follows the rate on the claim's date.
  return <td data-testid="claim-due">{claim.settledDue ?? due(claim.amount, rateFor(rates, person?.grade, claim.date))}</td>;
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const { people, claims, rates, today } = useSheetData(sheetId);
  return <td data-testid="person-due-total">{personDue(people.find((p) => p.id === personId), claims, rates, today)}</td>;
}

function DueTotal({ sheetId }: { sheetId: string }) {
  const { people, claims, rates, today } = useSheetData(sheetId);
  return (
    <p data-testid="rollup-due">
      Due total <strong data-testid="rollup-due-total">{sumDue(people.map((p) => personDue(p, claims, rates, today)))}</strong>
    </p>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  PersonHeaders.fill({ order: 15, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 15, Component: PersonDueTotal }),
  SheetPanel.fill({ order: 25, Component: DueTotal }),
];
