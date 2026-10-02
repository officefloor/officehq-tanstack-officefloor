import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The one collection the global search box reads: every client and every project, in a single
// payload served by GET /api/search. The box filters this by name on the client — a search outlives
// a click, so the term lives in the URL, read directly by the box (CLAUDE.md rules 4 & 5).

export type SearchClient = { id: number; name: string; email: string; archived: boolean };
export type SearchProject = {
  id: number;
  name: string;
  clientId: number;
  clientName: string;
  archived: boolean;
};
export type SearchResult = { clients: SearchClient[]; projects: SearchProject[] };

export const searchKey = ['search'] as const;

export function useSearchData() {
  return useQuery({
    queryKey: searchKey,
    queryFn: () => getJson<SearchResult>('/api/search'),
  });
}
