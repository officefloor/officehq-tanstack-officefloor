import { useQuery } from '@tanstack/react-query';
import { getLiveJson } from '../../api/http';
import { sheetKey, useCurrentSheet, useCurrentUser } from '../session/session';

/** A column of the people list a user may switch off, with the label its checkbox shows. */
export const COLUMNS = [
  ['claimed', 'Claimed'],
  ['due-total', 'Due'],
  ['excess', 'Excess'],
  ['statement-count', 'Statements'],
  ['stated-total', 'Stated total'],
  ['statement-date', 'Statement date'],
  ['overdue-count', 'Overdue'],
  ['overdue-total', 'Overdue total'],
  ['patrons', 'Patrons'],
  ['backing', 'Backing'],
  ['hold', 'Hold'],
  ['retired', 'Retired'],
  ['terms', 'Terms'],
  ['contact', 'Contact'],
  ['aliases', 'Aliases'],
] as const;
export type Column = (typeof COLUMNS)[number][0];

export type UserColumns = { user: string; hidden: string[] };

/** The query key for the columns the signed-in user switched off; under sheetKey, so re-loading the sheet re-reads it. */
export const columnsKey = (sheet: string, user: string) => [...sheetKey(sheet), 'columns', user] as const;

/** The columns of the people list the signed-in user switched off on this sheet: none unless chosen. */
export function useUserColumns() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: columnsKey(sheet, user),
    queryFn: () =>
      getLiveJson<UserColumns>(`/api/sheets/${encodeURIComponent(sheet)}/columns?user=${encodeURIComponent(user)}`),
    enabled: !!sheet && !!user,
  });
}

/** Whether the signed-in user's screen shows the column: a column switched off has no cell at all. */
export function useColumnShown(column: Column): boolean {
  return !(useUserColumns().data?.hidden ?? []).includes(column);
}
