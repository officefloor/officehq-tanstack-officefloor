import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A void reason on the sheet, with how many VOID claims were voided with it. */
export type VoidReason = { reason: string; voided: number };

export const voidReasonsKey = (sheet: string) => ['void-reasons', sheet] as const;

export const voidReasonsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/void-reasons`;

/** The sheet's void reasons, in the order added. While there is any, a void needs one chosen from them. */
export function useVoidReasons(sheet: string) {
  return useQuery({
    queryKey: voidReasonsKey(sheet),
    queryFn: () => getJson<VoidReason[]>(voidReasonsPath(sheet)),
  });
}
