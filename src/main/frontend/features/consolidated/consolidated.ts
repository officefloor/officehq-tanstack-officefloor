import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const familyKey = (sheet: string) => ['family', sheet] as const;

/** The sheet's id then those of all its descendants: its children (the sheets naming it as their parent), theirs,
 *  and so on, each once. */
export function useFamily(sheet: string) {
  return useQuery({
    queryKey: familyKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/family`),
  });
}
