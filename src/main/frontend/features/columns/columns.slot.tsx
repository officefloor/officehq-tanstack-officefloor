import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { COLUMNS, columnsKey, columnsPath, useHiddenColumns } from './columns';
import type { Column, UserColumns } from './columns';

// The signed-in user's choice of people-list columns: a checkbox per column, each saved as it is ticked. Each user
// has their own (all shown unless chosen); it changes nothing on the sheet, so it is not audited.
function ColumnChooser({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useHiddenColumns(sheet, user);
  const hidden = data?.hidden ?? [];

  const save = useMutation({
    mutationFn: (choice: { column: Column; shown: boolean }) =>
      postJson<UserColumns>(byUser(columnsPath(sheet), user), choice),
    onMutate: ({ column, shown }) => {
      // Shown at once; each press names only its own column, so presses in flight never undo one another.
      queryClient.setQueryData<UserColumns>(columnsKey(sheet, user), (old) => {
        const rest = (old?.hidden ?? []).filter((c) => c !== column);
        return { hidden: shown ? rest : [...rest, column] };
      });
    },
    onError: () => queryClient.invalidateQueries({ queryKey: columnsKey(sheet, user) }),
  });

  return (
    <fieldset>
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input
            type="checkbox"
            data-testid={`column-${c}`}
            checked={!hidden.includes(c)}
            disabled={data === undefined}
            onChange={(e) => save.mutate({ column: c, shown: e.target.checked })}
          />{' '}
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}

export const contribution = SheetPanels.fill({ order: 9.95, Component: ColumnChooser });
