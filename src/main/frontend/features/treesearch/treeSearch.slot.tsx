import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { searchTree } from './treeSearch';
import type { Found } from './treeSearch';

// The tree search: a box and a button list everyone, over this sheet and every other sheet of its
// tree, whose name or an alias holds the typed text (case ignored; blank: everyone), one line each
// as <sheet id> <name> <due-total>, this sheet's people first in list order, then the other sheets'
// by sheet id. Always read fresh; the count is cleared while it runs.
function TreeSearch({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const [found, setFound] = useState<Found[] | null>(null);
  const [error, setError] = useState('');

  const search = useMutation({
    mutationFn: (t: string) => searchTree(sheetId, t),
    onSuccess: setFound,
    onError: () => setError('The search could not be run.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setFound(null);
    setError('');
    search.mutate(text.trim());
  };

  return (
    <section data-testid="tree-search">
      <form onSubmit={submit}>
        <label>
          Search the tree{' '}
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
        </button>
        {found !== null && (
          <>
            {' '}Found <span data-testid="search-count">{found.length}</span>
            <ul>
              {found.map((f, i) => (
                <li key={i} data-testid="search-line">
                  {f.sheetId} {f.name} {f.dueTotal}
                </li>
              ))}
            </ul>
          </>
        )}
        {error && (
          <p role="alert" data-testid="search-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 2.5, Component: TreeSearch });
