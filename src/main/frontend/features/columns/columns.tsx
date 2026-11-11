import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useUser } from '../session/session';

// The people-list columns a user can switch off on their own screen. Each is kept per user, not
// per sheet; a column switched off has no cell (and no header cell) at all, and every figure is
// worked out as before.
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
export type ColumnKey = (typeof COLUMNS)[number];

export const columnsKey = (user: string) => ['user', user, 'columns'] as const;

/** The columns the signed-in user has switched off: none until they switch one off. */
export function useHiddenColumns(user: string) {
  return useQuery({
    queryKey: columnsKey(user),
    queryFn: async () => (await getJson<{ hidden: string[] }>('/api/columns', user)).hidden,
  });
}

/** Renders a people-list cell (or its header cell) only while the signed-in user shows `name`. */
export function Col({ name, children }: { name: ColumnKey; children: ReactNode }) {
  const [user] = useUser();
  const hidden = useHiddenColumns(user);
  return hidden.data?.includes(name) ? null : <>{children}</>;
}
