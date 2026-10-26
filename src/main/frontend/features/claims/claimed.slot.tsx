import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { claimsQuery, counts, sumAmounts } from './claimsQuery';

function useCounting(sheetId: string) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  return (useQuery(claimsQuery(sheetId)).data ?? []).filter((c) => counts(c, people));
}

function PersonClaimed({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useCounting(sheetId);
  return <td data-testid="person-claimed">{sumAmounts(claims.filter((c) => c.personId === personId))}</td>;
}

function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useCounting(sheetId);
  return (
    <p data-testid="rollup">
      Claimed total <strong data-testid="rollup-claimed-total">{sumAmounts(claims)}</strong>
    </p>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 10, Component: () => <th>Claimed</th> }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
  SheetPanel.fill({ order: 20, Component: ClaimedTotal }),
];
