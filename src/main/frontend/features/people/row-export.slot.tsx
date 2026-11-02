import { useRef } from 'react';
import { useIsFetching } from '@tanstack/react-query';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from './people';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The row's cells the export carries, in this order.
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

function ExportPersonHeader() {
  return <th />;
}

// The button that downloads the person's row as CSV: a header line "cell,value", then a line per cell above with the
// value exactly as the row shows it (blank when the row does not show that cell).
function PersonExport({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const cell = useRef<HTMLTableCellElement>(null);

  // While the screen's reads are in flight the values are not yet the ones shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = () => {
    const row = cell.current?.closest('tr');
    const shown = (id: string) => row?.querySelector(`[data-testid="${id}"]`)?.textContent ?? '';
    const lines = ['cell,value', ...CELLS.map((id) => [id, shown(id)].map(csvValue).join(','))];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `person-${sheet}-${person.id}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <td ref={cell}>
      <button type="button" data-testid="person-export" disabled={reading} onClick={download}>
        Export row (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 22, Component: ExportPersonHeader }),
  PersonColumns.fill({ order: 22, Component: PersonExport }),
];
