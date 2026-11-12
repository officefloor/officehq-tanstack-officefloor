import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A claim template: a claim made from it starts with its amount and note. */
export type Template = { name: string; amount: string; note: string };

/** Under the sheet's key, so loading the sheet again re-reads its templates. */
export const templatesKey = (sheet: string) => [...sheetKey(sheet), 'templates'] as const;

export function useTemplates(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(templatesKey(sheet), asOf),
    queryFn: () => getJson<Template[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/templates`, asOf)),
    enabled: sheet !== '',
  });
}
