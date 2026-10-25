import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet } from '../session/session';
import { csvValue } from './export.slot';
import type { Person } from './people';

// The cells a person's row export lists, in order.
const EXPORTED = [
  'person-name', 'person-grade', 'person-claimed', 'person-due-total', 'person-statement-count',
  'person-overdue-total', 'person-hold', 'person-retired', 'person-aliases', 'person-score',
] as const;

// A person's row export button: downloads the row as CSV — a header `cell,value`, then one line per cell above
// with its value as the row shows it (blank when the row does not show it). Quoting and line ends as the people
// export.
function RowExportHeader() {
  return <th>Export</th>;
}

function PersonRowExport({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const download = (button: HTMLButtonElement) => {
    const row = button.closest(`[data-testid="person-row-${person.id}"]`);
    const lines = [['cell', 'value']];
    for (const id of EXPORTED) {
      lines.push([id, row?.querySelector(`[data-testid="${id}"]`)?.textContent ?? '']);
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `person-${sheet ?? ''}-${person.id}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <td>
      <button type="button" data-testid="person-export" onClick={(e) => download(e.currentTarget)}>
        Export row (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 99, Component: RowExportHeader }),
  PersonCells.fill({ order: 99, Component: PersonRowExport }),
];
