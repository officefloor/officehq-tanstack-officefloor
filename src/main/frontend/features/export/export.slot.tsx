import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from '../people/people';

// Downloads a person's row as a CSV file: a `cell,value` header, then one line per cell below,
// each with the value exactly as the row shows it (blank when the row does not show the cell).
const CELLS = [
  'person-name',
  'person-grade',
  'person-claimed',
  'person-due-total',
  'person-statement-count',
  'person-overdue-total',
  'person-hold',
  'person-retired',
  'person-aliases',
  'person-score',
];

function csvField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The CSV for a rendered person row. */
export function rowCsv(row: Element): string {
  const lines = ['cell,value'];
  for (const cell of CELLS) {
    const el = row.querySelector(`[data-testid="${cell}"]`);
    lines.push(`${cell},${csvField(el?.textContent ?? '')}`);
  }
  return lines.join('\n') + '\n';
}

function ExportHead() {
  return <th />;
}

function PersonExport({ person }: { person: Person }) {
  return (
    <td>
      <button
        type="button"
        data-testid="person-export"
        onClick={(e) => {
          const row = e.currentTarget.closest('tr');
          if (!row) return;
          const url = URL.createObjectURL(new Blob([rowCsv(row)], { type: 'text/csv' }));
          const a = document.createElement('a');
          a.href = url;
          a.download = `person-${person.id}.csv`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          setTimeout(() => URL.revokeObjectURL(url), 0);
        }}
      >
        Export
      </button>
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 210, Component: ExportHead }),
  PersonColumns.fill({ order: 210, Component: PersonExport }),
];
