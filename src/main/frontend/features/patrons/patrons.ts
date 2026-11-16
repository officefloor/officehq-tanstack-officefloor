import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** That the person `personId` has named `patronId`, another person on the sheet, as a patron. */
export type Patron = { personId: number; patronId: number };

export const patronsKey = (sheetId: string) => ['sheet', sheetId, 'patrons'];

/** Every patron named on the sheet. */
export function usePatrons(sheetId: string) {
  return useQuery({
    queryKey: patronsKey(sheetId),
    queryFn: () => getJson<Patron[]>(`/api/sheets/${encodeURIComponent(sheetId)}/patrons`),
  });
}
