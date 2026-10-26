import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';

// The audit viewer: reads the sheet's lines from the audit file as it holds them, newest first. A name in the
// filter keeps only the lines about that person (name=/person=, compared as names are). It records nothing.
function AuditViewPanel({ sheetId }: { sheetId: string; user: string }) {
  const [filter, setFilter] = useState('');
  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`),
  });

  return (
    <section data-testid="audit-panel">
      <h2>Audit</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          show.reset();
          show.mutate(filter);
        }}
      >
        <input
          data-testid="audit-filter"
          placeholder="Person name (blank: all)"
          value={filter}
          onChange={(e) => {
            setFilter(e.target.value);
            show.reset();
          }}
        />{' '}
        <button type="submit" data-testid="audit-show" disabled={show.isPending}>
          Show
        </button>
        {show.isError && (
          <span data-testid="audit-error" role="alert">
            {' '}
            {show.error.message}
          </span>
        )}
      </form>
      <ul data-testid="audit-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 9.5, Component: AuditViewPanel });
