import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, asString, useSearchParam } from '../../url/useSearchParam';
import { sheetKey } from '../sheet/sheet';

/** `version` moves on with each stored edit; an edit is saved only over the version its editor loaded.
 *  `statementCount` is how many statements were taken of them; `statedTotal` (two decimals, or n/a)
 *  and `statementDate` (yyyy-MM-dd) are the last one's, null when none was taken. `retired` is set
 *  as soon as a `retiredFrom` date (yyyy-MM-dd, null when not retired) is, whatever the date.
 *  While `held`, their OPEN claims count in no live total; their SETTLED ones still do.
 *  `patronIds` are the ids of the other people on the sheet they named as their patrons. */
export type Person = {
  id: number;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
  retired: boolean;
  retiredFrom: string | null;
  held: boolean;
  patronIds: number[];
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

/** Whether the people list shows retired people too; off when the screen is first opened. */
export const useShowRetired = () => useSearchParam('showRetired', asFlag);
