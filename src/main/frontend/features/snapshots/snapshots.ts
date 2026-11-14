import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** That the person `personId`'s due-total stood at `dueTotal` (as shown) as the sheet left `day`. */
export type Snapshot = { personId: number; day: string; dueTotal: string };

/** The query key for the sheet's snapshots; under sheetKey, so re-loading the sheet or moving its today re-reads them. */
export const snapshotsKey = (sheet: string) => [...sheetKey(sheet), 'snapshots'] as const;

/** Every person's snapshot lines on the sheet, by person and oldest first. */
export function useSnapshots() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: snapshotsKey(sheet),
    queryFn: () => getJson<Snapshot[]>(`/api/sheets/${encodeURIComponent(sheet)}/snapshots`),
    enabled: !!sheet,
  });
}
