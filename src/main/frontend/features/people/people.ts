import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { sheetKey } from '../sheet/sheet';

/** `version` moves on with each stored edit; an edit is saved only over the version its editor loaded.
 *  `statementCount` is how many statements were taken of them; `statedTotal` (two decimals, or n/a)
 *  and `statementDate` (yyyy-MM-dd) are the last one's, null when none was taken. */
export type Person = {
  id: number;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
};

export const GRADES = ['A', 'B', 'C'] as const;

/** Under the sheet's key, so loading the sheet again re-reads its people. */
export const peopleKey = (sheet: string) => [...sheetKey(sheet), 'people'] as const;

export function usePeople(sheet: string) {
  return useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: sheet !== '',
  });
}

/** The id of the person whose editor is open in the person form ('' when it adds). */
export const useEditingPerson = () => useSearchParam('editPerson', asString);
