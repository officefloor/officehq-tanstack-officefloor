import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** That the person `personId` names `patronId`, another person on the sheet, as a patron. */
export type Patron = { personId: number; patronId: number };

/** The query key for the sheet's patrons; under sheetKey, so re-loading the sheet re-reads them. */
export const patronsKey = (sheet: string) => [...sheetKey(sheet), 'patrons'] as const;

/** Every person's patrons on the sheet. */
export function usePatrons() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: patronsKey(sheet),
    queryFn: () => getJson<Patron[]>(`/api/sheets/${encodeURIComponent(sheet)}/patrons`),
    enabled: !!sheet,
  });
}

/** The ids of the person's patrons. */
export const patronIdsOf = (patrons: Patron[], personId: number) =>
  patrons.filter((p) => p.personId === personId).map((p) => p.patronId);
