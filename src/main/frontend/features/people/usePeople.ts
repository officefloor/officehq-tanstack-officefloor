import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `version` goes up on every save; an editor sends back the version it loaded, so a save over
 *  someone else's newer one is refused. `statementCount` is how many statements were taken;
 *  `statedTotal` is the last one's due-total ("n/a" possible, "" when none) and `statementDate` the
 *  sheet's today when it was taken ("" when none). */
export type Person = {
  id: string;
  name: string;
  grade: string;
  version: number;
  statementCount: number;
  statedTotal: string;
  statementDate: string;
};

export const GRADES = ['A', 'B', 'C'] as const;

export const peopleKey = (sheet: string) => ['sheet', sheet, 'people'] as const;

/** The sheet's people, in the order they were added. */
export function usePeople(sheet: string): Person[] {
  const { data } = useQuery({
    queryKey: peopleKey(sheet),
    queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
