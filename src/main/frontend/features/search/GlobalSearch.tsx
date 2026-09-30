import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import type { Client } from '../clients/ClientsPage';
import type { Project } from '../projects/ProjectsPage';

// One search box across both clients and projects. The server does the matching (GET /api/search)
// and hands back the two groups; this component just renders them, grouped by kind.
type SearchResults = { clients: Client[]; projects: Project[] };

// The global search box and its results. It OWNS the `search` URL key (rule 4: a search term
// outlives a click, so it lives in the URL, not useState). The results are read with useQuery under
// ['search', term] — server data, never copied into state (rule 5) — and only fetched once there is
// something to look for. Carries data-testid="global-search" (the test contract), with the matches
// grouped under data-testid="search-clients" and data-testid="search-projects".
export function GlobalSearch() {
  const [q, setQ] = useSearchParam('search', asString);
  const term = q.trim();
  const results = useQuery({
    queryKey: ['search', term],
    queryFn: () => getJson<SearchResults>(`/api/search?q=${encodeURIComponent(term)}`),
    enabled: term.length > 0,
  });

  const clients = results.data?.clients ?? [];
  const projects = results.data?.projects ?? [];

  return (
    <div data-testid="global-search-panel">
      <input
        data-testid="global-search"
        type="search"
        placeholder="Search clients and projects"
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      {term.length > 0 && (
        <div data-testid="search-results">
          <section data-testid="search-clients">
            {clients.map((c) => (
              <div key={c.id} data-testid={`client-row-${c.id}`}>
                <span data-testid="client-name">{c.name}</span>
                <span data-testid="client-email">{c.email}</span>
              </div>
            ))}
          </section>
          <section data-testid="search-projects">
            {projects.map((p) => (
              <div key={p.id} data-testid={`project-row-${p.id}`}>
                <span data-testid="project-name">{p.name}</span>
                <span data-testid="project-client">{p.clientName}</span>
              </div>
            ))}
          </section>
        </div>
      )}
    </div>
  );
}
