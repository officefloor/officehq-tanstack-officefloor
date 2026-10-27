import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';

/** The audit viewer: the sheet's audit lines exactly as the audit file holds them, newest first. With a name in the
 *  filter, only the lines about that person (name=<it> or person=<it>, case and surrounding spaces ignored). */
function AuditView({ sheetId }: { sheetId: string }) {
  const [filter, setFilter] = useState('');
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');

  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<string[]>(
        `/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`,
      ),
    onSuccess: (found) => setLines(found),
    onError: (e: Error) => setError(e.message || 'The audit lines could not be read.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLines([]);
    show.mutate(filter.trim());
  };

  return (
    <section data-testid="audit-view">
      <h3>Audit</h3>
      <form onSubmit={submit}>
        <label>
          Person{' '}
          <input
            data-testid="audit-filter"
            value={filter}
            onChange={(e) => {
              setFilter(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="audit-show" disabled={show.isPending}>
          Show
        </button>
        {error && <p data-testid="audit-error">{error}</p>}
      </form>
      <ol data-testid="audit-lines">
        {lines.map((line, i) => (
          <li key={i} data-testid="audit-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

// Keyed by sheet, so a filter or the lines shown never carry over to another sheet.
export const contribution = SheetPanel.fill({
  order: 40,
  Component: ({ sheetId }) => <AuditView key={`audit-view-${sheetId}`} sheetId={sheetId} />,
});
