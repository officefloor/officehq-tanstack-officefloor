import { useQuery } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { figure, ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';

/**
 * Downloads the person's statement as CSV: one line per claim of theirs bound by their last statement, oldest first,
 * with the figure the claim shows (an empty number, and n/a, as they are). Just the header when none is bound.
 */
function ExportStatement({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const exportCsv = () => {
    const person = people.find((p) => p.id === personId);
    downloadCsv(`statement-${personId}.csv`, [
      ['number', 'date', 'amount', 'figure', 'status'],
      ...claims
        .filter((c) => c.personId === personId && c.stated)
        .reverse()
        .map((c) => [c.number ?? '', c.date, c.amount, figure(c, rates, person, floor, late), c.status]),
    ]);
  };

  return (
    <td>
      <button type="button" data-testid="person-export-statement" onClick={exportCsv}>
        Export statement (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 17.6, Component: () => <th>Statement export</th> }),
  PersonCells.fill({ order: 17.6, Component: ExportStatement }),
];
