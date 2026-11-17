/** A CSV value: wrapped in double quotes, any quote inside doubled, when it holds a comma, a quote or a line break. */
export function csvValue(v: string): string {
  return /[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
}

/** Downloads `lines` as a CSV file named `name`, each line ended by a line feed. */
export function download(name: string, lines: string[]) {
  const url = URL.createObjectURL(new Blob([lines.join('\n') + '\n'], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
