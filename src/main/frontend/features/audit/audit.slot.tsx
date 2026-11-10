import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';

// The audit viewer: a filter entry, a show button and the sheet's audit lines, read from the audit
// file exactly as it holds them, newest first. A name in the filter keeps only the lines about that
// person (name= or person= that name, case and surrounding spaces ignored). It writes nothing.
function AuditViewer({ sheetId }: { sheetId: string }) {
  const [user] = useUser();
  const [filter, setFilter] = useState('');
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');

  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<{ lines: string[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`,
        user,
      ),
    onSuccess: (data) => setLines(data.lines),
    onError: () => setError('The audit could not be read.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLines([]);
    show.mutate(filter);
  };

  return (
    <section data-testid="audit-viewer">
      <h2>Audit</h2>
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
        {error && (
          <p role="alert" data-testid="audit-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="audit-lines">
        {lines.map((line, i) => (
          <li key={i} data-testid="audit-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 40, Component: AuditViewer });
