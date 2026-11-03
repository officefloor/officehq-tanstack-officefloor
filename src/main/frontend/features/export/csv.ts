/** A query re-read now: its fresh data (as of the screen's as-of date, when one is set), else what it held. */
export async function fresh<T>(q: { data: T | undefined; refetch: () => Promise<{ data: T | undefined }> }): Promise<T | undefined> {
  return (await q.refetch()).data ?? q.data;
}

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
 *  or a line break. */
function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
}

/** Download these lines as a CSV file of this name, each line ending with a line feed. */
export function downloadCsv(lines: string[][], filename: string) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
