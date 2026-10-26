import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts, sumAmounts } from './claimsQuery';

function PersonClaimed({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = (useQuery(claimsQuery(sheetId)).data ?? []).filter(counts);
  return <td data-testid="person-claimed">{sumAmounts(claims.filter((c) => c.personId === personId))}</td>;
}

function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = (useQuery(claimsQuery(sheetId)).data ?? []).filter(counts);
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
