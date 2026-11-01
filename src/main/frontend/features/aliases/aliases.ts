import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** One of `person`'s aliases: another name the quick-add bar and the paste box find them by. */
export type Alias = { person: number; alias: string };

export const aliasesKey = (sheet: string) => ['aliases', sheet] as const;

export const aliasesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/aliases`;

/** Every alias on the sheet. */
export function useAliases(sheet: string) {
  return useQuery({
    queryKey: aliasesKey(sheet),
    queryFn: () => getJson<Alias[]>(aliasesPath(sheet)),
  });
}

/** Orders names as they are compared: without regard to case. */
export const byNameKey = (a: string, b: string) => {
  const x = a.toLowerCase();
  const y = b.toLowerCase();
  return x < y ? -1 : x > y ? 1 : 0;
};
