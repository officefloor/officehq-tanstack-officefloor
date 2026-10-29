import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';

/** The tree search: every person on this sheet and the other sheets of its tree whose name or an alias contains the
 *  typed text (case ignored), one line each as `<sheet id> <name> <due-total>`: this sheet's people first in list
 *  order, then the other sheets' by sheet id. */
function TreeSearch({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');

  const search = useMutation({
    mutationFn: (typed: string) =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/search?text=${encodeURIComponent(typed)}`),
    onSuccess: (found) => setLines(found),
    onError: (e: Error) => setError(e.message || 'The search could not be run.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLines(null);
    search.mutate(text);
  };

  return (
    <section data-testid="tree-search">
      <h3>Search the tree</h3>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input
            data-testid="search-name"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="search" disabled={search.isPending}>
          Search
        </button>{' '}
        {lines && <span>Found <strong data-testid="search-count">{lines.length}</strong></span>}
        {error && <p data-testid="search-error">{error}</p>}
      </form>
      <ol data-testid="search-lines">
        {(lines ?? []).map((line, i) => (
          <li key={i} data-testid="search-line">
            {line}
          </li>
        ))}
      </ol>
    </section>
  );
}

// Keyed by sheet, so the text or the lines shown never carry over to another sheet.
export const contribution = SheetPanel.fill({
  order: 39,
  Component: ({ sheetId }) => <TreeSearch key={`tree-search-${sheetId}`} sheetId={sheetId} />,
});
