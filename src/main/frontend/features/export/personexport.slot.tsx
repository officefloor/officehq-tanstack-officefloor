import { useRef } from 'react';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { downloadCsv } from './csv';

// The row cells the person export carries, in its order.
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

function PersonExportHead() {
  return <th>Export</th>;
}

// Downloads the person's row as a CSV file: a header line `cell,value`, then one line per cell with
// the value exactly as the row shows it ('' for a cell the row does not show). Quoted and ended as
// the people export.
function PersonExport({ sheetId, person }: { sheetId: string; person: Person }) {
  const cell = useRef<HTMLTableCellElement>(null);

  const download = () => {
    const row = cell.current?.closest('tr');
    const lines = [['cell', 'value']];
    for (const id of CELLS) lines.push([id, row?.querySelector(`[data-testid="${id}"]`)?.textContent ?? '']);
    downloadCsv(`person-${sheetId}-${person.id}.csv`, lines);
  };

  return (
    <td ref={cell}>
      <button type="button" data-testid="person-export" onClick={download}>
        Export (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 67.5, Component: PersonExportHead }),
  PersonRow.fill({ order: 67.5, Component: PersonExport }),
];
