import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A person's patron: another person on the same sheet (by id). */
export type Patron = { personId: number; patronId: number };

export const patronsKey = (sheetId: string) => ['sheet', sheetId, 'patrons'] as const;

/** Every person's patrons on the sheet, each person's in the order they were named. */
export function usePatrons(sheetId: string) {
  return useQuery({
    queryKey: patronsKey(sheetId),
    queryFn: () => getJson<Patron[]>(`/api/sheets/${encodeURIComponent(sheetId)}/patrons`),
  });
}
