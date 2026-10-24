import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';
import { asString, useSearchParam } from '../../url/useSearchParam';

export const GRADES = ['A', 'B', 'C'] as const;

// `version` is raised on every save; an edit sends the version it loaded and is refused (409) when
// someone else has saved the person since.
// `statementCount` is how many statements were taken for them; `statedTotal` and `statementDate` are the last
// one's due-total and the sheet's today when it was taken (null with none).
export type Person = {
  id: number;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string | null;
  statementDate: string | null;
};

export const peopleQueryKey = (sheetId: string) => ['sheet', sheetId, 'people'] as const;

export const peoplePath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/people`;

/** The sheet's people, in the order they were added. */
export function usePeople() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: peopleQueryKey(sheet ?? ''),
    queryFn: () => getJson<Person[]>(peoplePath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The id of the person whose editor is open (search param `editPerson`), or '' for none. */
export const useEditingPerson = () => useSearchParam('editPerson', asString);
