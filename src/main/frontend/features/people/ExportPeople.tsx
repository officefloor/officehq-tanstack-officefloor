import type { MouseEvent } from 'react';

/** One CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a
 *  quote or a line break. */
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

/** Downloads `lines` (the header line first) as the CSV file `name`, each line ending with a line feed. */
export function downloadCsv(name: string, lines: string[][]) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}

/** The "export people" button: downloads `rows` as a CSV file under a header line, each line
 *  ending with a line feed. Disabled while the figures are still being read (as when the as-of date
 *  has just changed), so the file holds the figures the screen shows once they are in. */
export function ExportPeople({ rows, loading }: { rows: () => string[][]; loading: boolean }) {
  const download = () => downloadCsv('people.csv', [['name', 'grade', 'retired-from', 'due-total'], ...rows()]);
  return (
    <button type="button" data-testid="export-people" disabled={loading} onClick={download}>
      Export people
    </button>
  );
}

/** The "export claims" button: downloads `rows` as a CSV file under a header line, quoted, ended
 *  and disabled as the people export. */
export function ExportClaims({ rows, loading }: { rows: () => string[][]; loading: boolean }) {
  const download = () => downloadCsv('claims.csv', [['person', 'number', 'date', 'amount', 'due', 'status', 'period'], ...rows()]);
  return (
    <button type="button" data-testid="export-claims" disabled={loading} onClick={download}>
      Export claims
    </button>
  );
}

/** The person's "export statement" button: downloads `rows` (their claims bound by their last
 *  statement, oldest first) as a CSV file under a header line, quoted, ended and disabled as the
 *  people export. */
export function ExportStatement({ name, rows, loading }: { name: string; rows: () => string[][]; loading: boolean }) {
  const download = () => downloadCsv(`statement-${name}.csv`, [['number', 'date', 'amount', 'figure', 'status'], ...rows()]);
  return (
    <button type="button" data-testid="person-export-statement" disabled={loading} onClick={download}>
      Export statement
    </button>
  );
}

/** The "export periods" button: downloads `rows` (one per person and period: people in list order,
 *  periods in the order added) as a CSV file under a header line, quoted, ended and disabled as the
 *  people export. */
export function ExportPeriods({ rows, loading }: { rows: () => string[][]; loading: boolean }) {
  const download = () => downloadCsv('periods.csv', [['person', 'period', 'figure', 'carried'], ...rows()]);
  return (
    <button type="button" data-testid="export-periods" disabled={loading} onClick={download}>
      Export periods
    </button>
  );
}

/** The cells a person's export holds, in file order. */
const PERSON_CELLS = ['person-name', 'person-grade', 'person-claimed', 'person-due-total', 'person-statement-count',
  'person-overdue-total', 'person-hold', 'person-retired', 'person-aliases', 'person-score'];

/** The person's "export" button: downloads their row as a CSV file of `cell,value` lines, each value
 *  read from the row as it is shown (blank for a cell the row does not show). Disabled as the people
 *  export. */
export function ExportPerson({ name, loading }: { name: string; loading: boolean }) {
  const download = (e: MouseEvent<HTMLButtonElement>) => {
    const row = e.currentTarget.closest('tr');
    const value = (cell: string) => row?.querySelector(`[data-testid="${cell}"]`)?.textContent ?? '';
    downloadCsv(`person-${name}.csv`, [['cell', 'value'], ...PERSON_CELLS.map((c) => [c, value(c)])]);
  };
  return (
    <button type="button" data-testid="person-export" disabled={loading} onClick={download}>
      Export
    </button>
  );
}
