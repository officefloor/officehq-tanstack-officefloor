// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote
// or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads `lines` as a CSV file named `name`, each line ending with a line feed.
export function downloadCsv(name: string, lines: string[][]) {
  const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
