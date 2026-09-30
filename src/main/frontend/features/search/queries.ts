import { getJson } from '../../api/http';

// The one global search box looks across BOTH clients and projects. It reads the server's combined
// result under a per-query key (CLAUDE.md rule 5) — the box queries for itself; nothing hands it
// data. The server does the matching so "one search box across both" is a single request.
export type SearchClient = { id: number; name: string; email: string };
export type SearchProject = { id: number; name: string; clientName: string };
export type SearchResults = { clients: SearchClient[]; projects: SearchProject[] };

// The URL search-param key the box owns (CLAUDE.md rule 4): the query text outlives a click, so it
// lives in the URL. A constant so any reader agrees on the one key without importing the box.
export const GLOBAL_SEARCH_PARAM = 'q';

// One query key per search text, so react-query caches each distinct search and refetches when the
// text changes.
export const searchKey = (query: string) => ['search', query] as const;

export function fetchSearch(query: string): Promise<SearchResults> {
  return getJson<SearchResults>(`/api/search?q=${encodeURIComponent(query)}`);
}
