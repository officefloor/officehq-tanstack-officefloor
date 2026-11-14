/** One CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a
 *  quote or a line break. */
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

/** The "export people" button: downloads `rows` as a CSV file under a header line, each line
 *  ending with a line feed. */
export function ExportPeople({ rows }: { rows: () => string[][] }) {
  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total'], ...rows()];
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'people.csv';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };
  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export people
    </button>
  );
}
