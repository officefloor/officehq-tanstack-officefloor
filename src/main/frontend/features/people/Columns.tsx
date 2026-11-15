import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';

/** The columns of the people list a user can switch off, in the order their checkboxes show. */
export const COLUMNS = [
  'claimed', 'due-total', 'excess', 'statement-count', 'stated-total', 'statement-date', 'overdue-count',
  'overdue-total', 'patrons', 'backing', 'hold', 'retired', 'terms', 'contact', 'aliases',
] as const;
export type Column = (typeof COLUMNS)[number];
type UserColumns = { user: string; sheetId: string; hidden: string[] };

const columnsKey = (sheetId: string, user: string) => ['user', user, 'columns', sheetId] as const;
const path = (user: string) => `/api/users/${encodeURIComponent(user)}/columns`;

/** Whether each column is shown on the user's screen for the sheet; every one until read. */
export function useColumns(sheetId: string, user: string): (c: Column) => boolean {
  const q = useQuery({
    queryKey: columnsKey(sheetId, user),
    queryFn: () => getJson<UserColumns>(`${path(user)}?sheet=${encodeURIComponent(sheetId)}`),
  });
  const hidden = new Set(q.data?.hidden ?? []);
  return (c) => !hidden.has(c);
}

/** A checkbox per column of the people list: saved for the signed-in user on this sheet only.
 *  The screen follows the box at once; each column is saved on its own, so quick clicks cannot
 *  undo one another. */
export function ColumnChooser({ sheetId, user }: { sheetId: string; user: string }) {
  const qc = useQueryClient();
  const shown = useColumns(sheetId, user);
  const key = columnsKey(sheetId, user);
  const save = useMutation({
    mutationKey: key,
    mutationFn: (v: { column: Column; shown: boolean }) =>
      putJson<UserColumns>(`${path(user)}/${v.column}?sheet=${encodeURIComponent(sheetId)}`, { shown: v.shown }),
    onMutate: async (v) => {
      await qc.cancelQueries({ queryKey: key });
      qc.setQueryData<UserColumns>(key, (d) => {
        const hidden = (d?.hidden ?? []).filter((c) => c !== v.column);
        return { user, sheetId, hidden: v.shown ? hidden : [...hidden, v.column] };
      });
    },
    // Read back once the last click is saved, so an earlier answer does not undo a later click.
    onSettled: () => (qc.isMutating({ mutationKey: key }) <= 1 ? qc.invalidateQueries({ queryKey: key }) : undefined),
  });
  return (
    <p>
      Columns{' '}
      {COLUMNS.map((c) => (
        <label key={c}>
          <input type="checkbox" data-testid={`column-${c}`} checked={shown(c)}
            onChange={(e) => save.mutate({ column: c, shown: e.target.checked })} />{' '}
          {c}{' '}
        </label>
      ))}
    </p>
  );
}
