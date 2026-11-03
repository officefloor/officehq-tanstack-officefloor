import { useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { COLUMNS, columnsKey, useHiddenColumns, type UserColumns } from './useColumns';

/** A checkbox per people-list column: which of them the signed-in user's screen shows. It is kept for the user,
 *  whatever sheet is loaded, changes no figure, and writes no audit line. A tick shows at once; the saves go to
 *  the store one after another, each with the whole choice, so the last one kept is the last one made. */
function ColumnChooser({ user }: { user: string }) {
  const hidden = useHiddenColumns();
  const queryClient = useQueryClient();
  const saving = useRef<Promise<unknown>>(Promise.resolve());

  const toggle = (column: string, shown: boolean) => {
    const current = queryClient.getQueryData<UserColumns>(columnsKey(user))?.hidden ?? [...hidden];
    const next = COLUMNS.filter((c) => (c === column ? !shown : current.includes(c)));
    queryClient.setQueryData<UserColumns>(columnsKey(user), { hidden: next });
    saving.current = saving.current
      .catch(() => undefined)
      .then(() => postJson<UserColumns>('/api/columns', { hidden: next }))
      .catch(() => queryClient.invalidateQueries({ queryKey: columnsKey(user) }));
  };

  return (
    <fieldset data-testid="columns-chooser">
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            data-testid={`column-${c}`}
            checked={!hidden.has(c)}
            onChange={(e) => toggle(c, e.target.checked)}
          />{' '}
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

export const contribution = SheetBody.fill({ order: -19, Component: ({ user }) => <ColumnChooser user={user} /> });
