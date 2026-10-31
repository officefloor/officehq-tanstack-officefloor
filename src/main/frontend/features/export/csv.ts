/** A CSV value: wrapped in double quotes, any quote inside doubled, when it holds a comma, a quote
 *  or a line break. */
export function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replaceAll('"', '""')}"` : v;
}

/** Downloads the lines as a CSV file of that name, each value quoted as needed and each line
 *  ending with a line feed. */
export function downloadCsv(lines: string[][], filename: string): void {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
