import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { sheetKey, useCurrentSheet } from '../session/session';

export const GRADES = ['A', 'B', 'C'] as const;

/** A person on the sheet as the store holds them; `version` counts their saves. */
export type Person = { id: number; name: string; grade: string; version: number };

/** The fields the person form submits. */
export type PersonInput = { name: string; grade: string };

/** The query key for the sheet's people; under sheetKey, so re-loading the sheet re-reads them. */
export const peopleKey = (sheet: string) => [...sheetKey(sheet), 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: !!sheet,
  });
}

/** The id of the person whose editor is open in the person form, if any. */
export const useEditingPerson = () => useSearchParam('person', asNumber);
