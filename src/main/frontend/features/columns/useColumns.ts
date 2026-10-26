import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { useUser } from '../session/useSession';

/** The people-list columns a user may switch off, in the order the chooser offers them. */
export const COLUMNS = [
  'claimed',
  'due-total',
  'excess',
  'statement-count',
  'stated-total',
  'statement-date',
  'overdue-count',
  'overdue-total',
  'patrons',
  'backing',
  'hold',
  'retired',
  'terms',
  'contact',
  'aliases',
] as const;
export type Column = (typeof COLUMNS)[number];

export type UserColumns = { user: string; hidden: string[] };

/** Kept per sheet and user: each user's screen chooses its own columns. */
export const columnsKey = (sheetId: string, user: string) => ['sheet', sheetId, 'columns', user] as const;

const path = (sheetId: string, user: string) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/columns/${encodeURIComponent(user)}`;

// The column choices still being saved, per sheet and user, as one chain: each choice is saved after the one before,
// and a read waits for the chain, so a re-load never shows the list from before a choice was saved.
const saving = new Map<string, Promise<unknown>>();

function useHiddenColumns(sheetId: string, user: string) {
  return useQuery({
    queryKey: columnsKey(sheetId, user),
    queryFn: async () => {
      await saving.get(JSON.stringify(columnsKey(sheetId, user)));
      return getJson<UserColumns>(path(sheetId, user));
    },
    enabled: sheetId !== '' && user !== '',
  });
}

/** Whether the signed-in user's screen shows the column (every column until switched off). */
export function useColumnShown(sheetId: string): (column: Column) => boolean {
  const [user] = useUser();
  const hidden = useHiddenColumns(sheetId, user ?? '').data?.hidden ?? [];
  return (column) => !hidden.includes(column);
}

/** Show or hide the column on the user's screen: at once on screen, then saved in turn. */
export function chooseColumn(queryClient: QueryClient, sheetId: string, user: string, column: Column, shown: boolean) {
  const key = columnsKey(sheetId, user);
  queryClient.setQueryData<UserColumns>(key, (now) => {
    const rest = (now?.hidden ?? []).filter((c) => c !== column);
    return { user, hidden: shown ? rest : [...rest, column] };
  });
  const id = JSON.stringify(key);
  const before = saving.get(id) ?? Promise.resolve();
  const done = before
    .catch(() => undefined)
    .then(() => putJson<UserColumns>(`${path(sheetId, user)}/${encodeURIComponent(column)}`, { shown }));
  saving.set(id, done);
  void done
    .catch(() => undefined)
    .then(() => {
      if (saving.get(id) !== done) return;
      saving.delete(id);
      return queryClient.invalidateQueries({ queryKey: key });
    });
}

export function useColumnChooser(sheetId: string) {
  const [user] = useUser();
  const name = user ?? '';
  const queryClient = useQueryClient();
  const hidden = useHiddenColumns(sheetId, name).data?.hidden ?? [];
  return {
    shown: (column: Column) => !hidden.includes(column),
    choose: (column: Column, shown: boolean) => chooseColumn(queryClient, sheetId, name, column, shown),
  };
}
