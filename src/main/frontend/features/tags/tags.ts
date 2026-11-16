import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { claimsKey } from '../claims/claims';

/** Under the claims' key, so whatever refreshes the claims refreshes the tags too. */
export const tagsKey = (sheetId: string) => [...claimsKey(sheetId), 'tags'];

/** Every tag ever used on the sheet's claims, sorted. */
export function useTags(sheetId: string) {
  return useQuery({
    queryKey: tagsKey(sheetId),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/tags`),
  });
}
