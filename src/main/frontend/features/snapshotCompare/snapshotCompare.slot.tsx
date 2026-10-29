import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';

/** The snapshot comparison: per person in list order, `<name> <then> <now>` — the due-totals of the SNAPSHOTs written
 *  on leaving the from-date (blank: the sheet's today) and the to-date (blank: the due-total now). A figure with no
 *  snapshot is empty, and each line is trimmed. Re-read from the store on every press. */
function SnapshotCompare({ sheetId }: { sheetId: string }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');

  const compare = useMutation({
    mutationFn: ({ from, to }: { from: string; to: string }) =>
      getJson<string[]>(
        `/api/sheets/${encodeURIComponent(sheetId)}/snapshots/compare?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      ),
    onSuccess: (found) => setLines(found),
    onError: (e: Error) => setError(e.message || 'The snapshots could not be compared.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLines(null);
    compare.mutate({ from: from.trim(), to: to.trim() });
  };

  return (
    <section data-testid="snapshot-comparison">
      <h3>Compare snapshots</h3>
      <form onSubmit={submit}>
        <label>
          From{' '}
          <input
            data-testid="snapshot-from"
            placeholder="yyyy-mm-dd"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="snapshot-to"
            placeholder="yyyy-mm-dd"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="snapshot-compare" disabled={compare.isPending}>
          Compare
        </button>
        {error && <p data-testid="snapshot-error">{error}</p>}
      </form>
      <ol data-testid="snapshot-lines">
        {(lines ?? []).map((line, i) => (
          <li key={i} data-testid="snapshot-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

// Keyed by sheet, so the dates or the lines shown never carry over to another sheet.
export const contribution = SheetPanel.fill({
  order: 39,
  Component: ({ sheetId }) => <SnapshotCompare key={`snapshot-compare-${sheetId}`} sheetId={sheetId} />,
});
