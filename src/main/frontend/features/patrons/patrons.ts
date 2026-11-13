import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * That the person `personId` names `patronId`, another person on the sheet, as a patron, with the patron's share
 * (two decimals) of the person's 100.00.
 */
export type Patron = { personId: number; patronId: number; share: string };

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

/** The person's patrons, in the order they were named. */
export const patronsOf = (patrons: Patron[], personId: number) => patrons.filter((p) => p.personId === personId);
