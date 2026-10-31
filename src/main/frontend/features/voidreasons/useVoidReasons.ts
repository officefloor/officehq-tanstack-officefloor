import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** A void reason as the server sends it: its text. */
export type VoidReason = { reason: string };

export const voidReasonsKey = (sheet: string) => ['sheet', sheet, 'void-reasons'] as const;

/** The sheet's void reasons, in the order added, re-read whenever the sheet is loaded. */
export function useVoidReasons(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(voidReasonsKey(sheet), asOf),
    queryFn: () => getJson<VoidReason[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}
