/** A CSV value: wrapped in double quotes, with inner quotes doubled, when it holds a comma, a quote or a line break. */
export function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Downloads the rows (each a list of values) as a CSV file, every line ending in "\n". */
export function downloadCsv(fileName: string, rows: string[][]): void {
  const text = rows.map((r) => r.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
