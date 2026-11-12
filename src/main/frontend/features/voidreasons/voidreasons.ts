import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A void reason on the sheet. While the sheet has any, a claim is voided with one of them. */
export type VoidReason = { reason: string };

/** Under the sheet's key, so loading the sheet again re-reads its void reasons. */
export const voidReasonsKey = (sheet: string) => [...sheetKey(sheet), 'void-reasons'] as const;

export function useVoidReasons(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(voidReasonsKey(sheet), asOf),
    queryFn: () => getJson<VoidReason[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/void-reasons`, asOf)),
    enabled: sheet !== '',
  });
}
