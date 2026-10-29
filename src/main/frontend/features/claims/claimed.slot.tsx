import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { claimsQuery, counts, sumAmounts } from './claimsQuery';
import { ColumnShown } from '../people/useColumns';

/** The claims that count toward claimed: those that count, leaving out claims that correct another. */
function useCounting(sheetId: string) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  return (useQuery(claimsQuery(sheetId)).data ?? []).filter((c) => c.corrects === null && counts(c, people, today));
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
  PersonHeaders.fill({ order: 10, Component: () => <ColumnShown column="claimed"><th>Claimed</th></ColumnShown> }),
  PersonCells.fill({ order: 10, Component: (ctx) => <ColumnShown column="claimed"><PersonClaimed {...ctx} /></ColumnShown> }),
  SheetPanel.fill({ order: 20, Component: ClaimedTotal }),
];
