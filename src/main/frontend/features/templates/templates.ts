import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's claim template as the store holds it: a trimmed name (on the sheet at most once), an amount the claim
 *  form accepts (two decimals) and a trimmed note. A claim made from it copies its amount and note at that moment. */
export type ClaimTemplate = { id: number; name: string; amount: string; note: string };

export const templatesQueryKey = (sheetId: string) => ['sheet', sheetId, 'templates'] as const;

export const templatesPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/templates`;

/** The path that adds a claim to a person from a template. */
export const templateClaimsPath = (sheetId: string, personId: number) =>
  `/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/template-claims`;

/** The sheet's claim templates, by name. */
export function useTemplates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: templatesQueryKey(sheet ?? ''),
    queryFn: () => getJson<ClaimTemplate[]>(templatesPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
