import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { listAudit } from './api';

/** The sheet's audit file, read on demand: every line (newest first), or with a name only those about
 *  that person. It shows what the file holds; it keeps nothing of its own. */
export function AuditViewer({ sheetId }: { sheetId: string; user: string }) {
  const [filter, setFilter] = useState('');
  const show = useMutation({ mutationFn: (name: string) => listAudit(sheetId, name) });

  return (
    <section>
      <h3>Audit</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          show.reset();
          show.mutate(filter);
        }}
      >
        <input data-testid="audit-filter" placeholder="Name (blank: all)" value={filter}
          onChange={(e) => setFilter(e.target.value)} />{' '}
        <button type="submit" data-testid="audit-show" disabled={show.isPending}>
          Show
        </button>
        {show.isError && <span data-testid="audit-error">The audit could not be read.</span>}
      </form>
      <ul>
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}
