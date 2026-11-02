import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A sheet's period: its name (unique on the sheet) and its from- and to-date (yyyy-mm-dd), both
 *  inclusive, whether it is closed, the due-total fixed when it was closed (null while open), and each
 *  person's due-total for it fixed then, by person id (empty while open). No two periods of a sheet overlap. */
export type Period = {
  id: number;
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
  personTotals: Record<string, string>;
};

export function periodsKey(sheetId: string) {
  return ['sheet', sheetId, 'periods'];
}

/** The sheet's periods, by from-date. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ periods: Period[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`)).periods,
  });
}

/** The period whose dates contain a date (yyyy-mm-dd); undefined when none does. */
export function periodOf(periods: Period[] | undefined, on: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= on && on <= p.to);
}
