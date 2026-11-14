import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { COLUMNS, columnsKey, useUserColumns } from './columns';
import type { Column, UserColumns } from './columns';

// A checkbox per column of the people list: the signed-in user's own choice of which their screen shows.
// Each box saves at once; the screen follows the box straight away and re-reads the store once every save is in.
export function ColumnChooser() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const hidden = useUserColumns().data?.hidden ?? [];
  const queryClient = useQueryClient();
  const key = columnsKey(sheet, user);
  const mutationKey = ['columns', sheet, user];

  const save = useMutation({
    mutationKey,
    mutationFn: ({ column, shown }: { column: Column; shown: boolean }) =>
      postJson<UserColumns>(
        `/api/sheets/${encodeURIComponent(sheet)}/columns?user=${encodeURIComponent(user)}`,
        { column, shown },
      ),
    onMutate: async ({ column, shown }) => {
      await queryClient.cancelQueries({ queryKey: key });
      queryClient.setQueryData<UserColumns>(key, (old) => {
        const rest = (old?.hidden ?? []).filter((c) => c !== column);
        return { user, hidden: shown ? rest : [...rest, column] };
      });
    },
    onSettled: async () => {
      if (queryClient.isMutating({ mutationKey }) <= 1) await queryClient.invalidateQueries({ queryKey: key });
    },
  });

  if (!user || !sheet) return null;

  return (
    <span>
      Columns:{' '}
      {COLUMNS.map(([column, label]) => (
        <label key={column}>
          <input
            type="checkbox"
            data-testid={`column-${column}`}
            checked={!hidden.includes(column)}
            onChange={(e) => save.mutate({ column, shown: e.target.checked })}
          />
          {label}{' '}
        </label>
      ))}
    </span>
  );
}
