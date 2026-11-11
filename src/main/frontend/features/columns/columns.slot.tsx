import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postOwnJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { COLUMNS, columnsKey, useHiddenColumns } from './columns';
import type { ColumnKey } from './columns';

// The signed-in user's column chooser: a checkbox per people-list column. A box shows its change at
// once; the choice is the user's own, kept across sheets, and writes no audit line.
function ColumnChooser({ user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const hidden = useHiddenColumns(user);
  const [error, setError] = useState('');

  const save = useMutation({
    mutationFn: ({ column, shown }: { column: ColumnKey; shown: boolean }) =>
      postOwnJson<{ hidden: string[] }>('/api/columns', { column, shown }, user),
    onError: () => {
      setError('That column choice could not be saved.');
      void queryClient.invalidateQueries({ queryKey: columnsKey(user) });
    },
  });

  const toggle = (column: ColumnKey, shown: boolean) => {
    setError('');
    void queryClient.cancelQueries({ queryKey: columnsKey(user) });
    queryClient.setQueryData<string[]>(columnsKey(user), (was = []) =>
      shown ? was.filter((c) => c !== column) : [...was.filter((c) => c !== column), column],
    );
    save.mutate({ column, shown });
  };

  return (
    <fieldset data-testid="columns">
      <legend>Columns</legend>
      {hidden.data &&
        COLUMNS.map((c) => (
          <label key={c}>
            <input
              type="checkbox"
              data-testid={`column-${c}`}
              checked={!hidden.data.includes(c)}
              onChange={(e) => toggle(c, e.target.checked)}
            />{' '}
            {c}{' '}
          </label>
        ))}
      {(error || hidden.isError) && (
        <p role="alert" data-testid="columns-error">
          {error || 'The columns could not be read.'}
        </p>
      )}
    </fieldset>
  );
}

export const contribution = SheetBody.fill({ order: -94, Component: ColumnChooser });
