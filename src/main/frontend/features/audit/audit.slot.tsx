import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';

/**
 * The audit viewer: the sheet's audit lines exactly as the audit file holds them, newest first.
 * A name in the filter lists only the lines about that person (name= or person= that name, case
 * and the spaces around it ignored). It reads the file; it is not a second record.
 */
function AuditPanel({ sheet }: { user: string; sheet: string }) {
  const [filter, setFilter] = useState('');
  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/audit?name=${encodeURIComponent(name)}`),
  });

  return (
    <section data-testid="audit-panel">
      <h2>Audit</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          // The old lines go while the new ones are read, so two presses are told apart.
          show.reset();
          show.mutate(filter);
        }}
      >
        <label>
          Person{' '}
          <input
            data-testid="audit-filter"
            placeholder="all"
            value={filter}
            onChange={(e) => {
              setFilter(e.target.value);
              show.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="audit-show" disabled={show.isPending}>
          Show
        </button>
        {show.isError && (
          <span data-testid="audit-error" role="alert">
            {' '}
            Could not read the audit file.
          </span>
        )}
      </form>
      <ol data-testid="audit-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">
            <code>{line}</code>
          </li>
        ))}
      </ol>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28, Component: AuditPanel });
