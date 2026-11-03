import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { useSignedInUser } from '../session/useSession';

/** The people-list columns a user may switch off, each named as its cell's `person-<column>` data-testid. */
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
export type UserColumns = { hidden: string[] };

/** The signed-in user's columns are theirs, not a sheet's: kept under its own key, not dropped when a sheet loads. */
export const columnsKey = (user: string) => ['user', user, 'columns'];

/** The columns the signed-in user switched off (none until they choose, and while it is being read). */
export function useHiddenColumns(): ReadonlySet<string> {
  const [user] = useSignedInUser();
  const columns = useQuery({
    queryKey: columnsKey(user),
    queryFn: () => getJsonNow<UserColumns>('/api/columns'),
    enabled: user !== '',
  });
  return new Set(columns.data?.hidden ?? []);
}

/** A people-list column's heading or cell: rendered only while the signed-in user has it switched on. */
export function Col({ id, children }: { id: Column; children: ReactNode }) {
  return useHiddenColumns().has(id) ? null : <>{children}</>;
}
