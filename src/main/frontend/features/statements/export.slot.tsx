import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { useCurrentSheet } from '../../session/session';
import { useClaims } from '../claims/claims';
import { lateOf } from '../overdue/overdue';
import { type Person } from '../people/people';
import { claimDue, useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

function StatementExportHead() {
  return <th>Statement CSV</th>;
}

// Downloads the person's statement as CSV: number, date, amount, figure and status of each of their
// claims bound by their last statement, oldest first (by date, then as added), the figure as the claim shows it.
function PersonStatementExport({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settingsQuery = useSettings(sheet);
  const settings = settingsQuery.data;

  const ready = !!claims.data && !!rates.data && !!settingsQuery.data && today !== undefined;

  const download = () => {
    const late = lateOf(settings, today, [person]);
    const bound = (claims.data ?? [])
      .filter((c) => c.personId === person.id && c.stated)
      .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.id - b.id));
    const lines = [['number', 'date', 'amount', 'figure', 'status']];
    for (const c of bound) {
      lines.push([c.number ?? '', c.date, c.amount, claimDue(c, rates.data, person.grades, floorOf(settings), late), c.status]);
    }
    const csv = lines.map((l) => l.map(cell).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `statement-${sheet}-${person.id}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <td>
      <button type="button" data-testid="person-export-statement" disabled={!ready} onClick={download}>
        Export statement (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 18.1, Component: StatementExportHead }),
  PersonColumns.fill({ order: 18.1, Component: PersonStatementExport }),
];
