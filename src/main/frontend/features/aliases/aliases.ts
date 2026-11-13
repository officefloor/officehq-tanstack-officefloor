import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** That the person `personId` is also known on the sheet by `alias`. */
export type Alias = { personId: number; alias: string };

/** The query key for the sheet's aliases; under sheetKey, so re-loading the sheet re-reads them. */
export const aliasesKey = (sheet: string) => [...sheetKey(sheet), 'aliases'] as const;

/** Every person's aliases on the sheet. */
export function useAliases() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: aliasesKey(sheet),
    queryFn: () => getJson<Alias[]>(`/api/sheets/${encodeURIComponent(sheet)}/aliases`),
    enabled: !!sheet,
  });
}

/** The person's aliases, sorted as names are compared (ignoring case). */
export const aliasesOf = (aliases: Alias[], personId: number) =>
  aliases
    .filter((a) => a.personId === personId)
    .map((a) => a.alias)
    .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
