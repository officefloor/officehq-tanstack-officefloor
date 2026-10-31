import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Person = { id: number; name: string; grade: string };

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people in the order they were added, re-read whenever the sheet is loaded. */
export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    staleTime: 0,
    gcTime: 0,
  });
}
