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
 *  ending with a line feed. */
export function ExportPeople({ rows }: { rows: () => string[][] }) {
  const download = () => downloadCsv('people.csv', [['name', 'grade', 'retired-from', 'due-total'], ...rows()]);
  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export people
    </button>
  );
}

/** The "export claims" button: downloads `rows` as a CSV file under a header line, quoted and
 *  ended as the people export. */
export function ExportClaims({ rows }: { rows: () => string[][] }) {
  const download = () => downloadCsv('claims.csv', [['person', 'number', 'date', 'amount', 'due', 'status', 'period'], ...rows()]);
  return (
    <button type="button" data-testid="export-claims" onClick={download}>
      Export claims
    </button>
  );
}
