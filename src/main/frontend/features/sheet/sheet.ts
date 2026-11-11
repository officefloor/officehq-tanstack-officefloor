import { useQuery } from '@tanstack/react-query';
import { getJsonUnpreviewed, previewDate } from '../../api/http';
import { usePreview } from './preview.slot';

export type Sheet = { id: string; today: string; archived: boolean };

export const sheetKey = (sheetId: string) => ['sheet', sheetId, 'info'] as const;

/** The loaded sheet: its id, its real today (yyyy-MM-dd; whatever preview date the screen shows) and whether a steward has archived it. */
export function useSheet(sheetId: string) {
  return useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => getJsonUnpreviewed<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
  });
}

/**
 * The sheet's today as the screen shows it: the preview date while one is set, else the sheet's
 * today. Every date-dependent figure on the screen uses this, never the real clock (`new Date()`);
 * '' until the sheet has been read.
 */
export function useSheetToday(sheetId: string): string {
  usePreview(); // re-render when the preview date changes
  const today = useSheet(sheetId).data?.today ?? '';
  const preview = previewDate();
  return today !== '' && preview !== '' ? preview : today;
}
