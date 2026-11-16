import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';

/** The audit viewer: the sheet's audit lines as the file holds them, newest first, read when Show is pressed; with a
 *  name in the filter, only the lines about that person (name=<it> or person=<it>, compared as names are), then the matching lines of the sheet's descendants. */
function AuditView({ sheetId }: { sheetId: string }) {
  const [filter, setFilter] = useState('');
  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`),
  });
  return (
    <section data-testid="audit-view">
      <h3>Audit</h3>
      <input
        data-testid="audit-filter"
        placeholder="name (blank: all)"
        value={filter}
        onChange={(e) => {
          setFilter(e.target.value);
          show.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="audit-show"
        disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate(filter.trim());
        }}
      >
        Show
      </button>
      {show.isError && <p data-testid="audit-error">Could not read the audit.</p>}
      <ul>
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 90, Component: AuditView });
