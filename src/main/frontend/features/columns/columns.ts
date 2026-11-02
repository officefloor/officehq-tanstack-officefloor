import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { byUser } from '../session/session';

/** The people-list columns a user may switch off on their own screen. A column switched off has no cell; every
 *  total, the header and the summary are figured the same whatever is shown. */
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

export type UserColumns = { hidden: Column[] };

export const columnsKey = (sheet: string, user: string) => ['columns', sheet, user] as const;

export const columnsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/columns`;

/** The columns the signed-in user switched off on the sheet (none unless chosen); read as they are now. */
export function useHiddenColumns(sheet: string, user: string) {
  return useQuery({
    queryKey: columnsKey(sheet, user),
    queryFn: () => getJsonNow<UserColumns>(byUser(columnsPath(sheet), user)),
    enabled: sheet !== '' && user !== '',
  });
}

/** Whether the signed-in user's screen shows a column: every column is shown unless switched off. */
export function useShown(sheet: string, user: string): (column: Column) => boolean {
  const { data } = useHiddenColumns(sheet, user);
  const hidden = data?.hidden ?? [];
  return (column) => !hidden.includes(column);
}
