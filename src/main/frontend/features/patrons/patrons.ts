import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** That `patron` is one of `person`'s patrons: both are ids of people on the sheet. */
export type Patron = { person: number; patron: number };

export const patronsKey = (sheet: string) => ['patrons', sheet] as const;

export const patronsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/patrons`;

/** Every patron named on the sheet. */
export function usePatrons(sheet: string) {
  return useQuery({
    queryKey: patronsKey(sheet),
    queryFn: () => getJson<Patron[]>(patronsPath(sheet)),
  });
}
