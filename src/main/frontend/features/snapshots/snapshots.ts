import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A SNAPSHOT written for the person `personId`: the day being left and their due-total on it. */
export type Snapshot = { personId: number; day: string; dueTotal: string };

export const snapshotsKey = (sheetId: string) => ['sheet', sheetId, 'snapshots'];

/** Every snapshot written on the sheet, oldest first. */
export function useSnapshots(sheetId: string) {
  return useQuery({
    queryKey: snapshotsKey(sheetId),
    queryFn: () => getJson<Snapshot[]>(`/api/sheets/${encodeURIComponent(sheetId)}/snapshots`),
  });
}
