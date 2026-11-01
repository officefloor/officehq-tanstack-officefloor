import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

/** A void reason as the server sends it: its text. */
export type VoidReason = { reason: string };

export const voidReasonsKey = (sheet: string) => ['sheet', sheet, 'void-reasons'] as const;

/** The sheet's void reasons, in the order added, re-read whenever the sheet is loaded. */
export function useVoidReasons(sheet: string) {
  const view = useView();
  return useQuery({
    queryKey: viewKey(voidReasonsKey(sheet), view),
    queryFn: () => getJson<VoidReason[]>(viewPath(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, view)),
    staleTime: 0,
    gcTime: 0,
  });
}
