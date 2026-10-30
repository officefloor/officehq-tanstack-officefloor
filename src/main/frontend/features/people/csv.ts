/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
 *  or a line break. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The rows as CSV text, each line ending with a line feed. */
export function toCsv(rows: string[][]): string {
  return rows.map((row) => row.map(csvValue).join(',') + '\n').join('');
}

/** Downloads the rows as a CSV file with the given name. */
export function downloadCsv(filename: string, rows: string[][]): void {
  const url = URL.createObjectURL(new Blob([toCsv(rows)], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
