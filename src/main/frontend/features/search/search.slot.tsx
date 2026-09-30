import { useQuery } from '@tanstack/react-query';
import { AppNav } from '../../slots/defs/appNav';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { GLOBAL_SEARCH_PARAM, fetchSearch, searchKey } from './queries';

// The one global search box, with its results grouped by kind. It fills the shell nav region (an
// existing region — CLAUDE.md rule 3), so a single box is present on every page and looks across
// both clients and projects. It owns the `q` URL key (rule 4) and queries the server's combined
// result for itself under ['search', q] (rule 5) — nothing hands it data. Results appear only once
// something has been typed.
function GlobalSearch() {
  const [query, setQuery] = useSearchParam(GLOBAL_SEARCH_PARAM, asString);
  const trimmed = query.trim();
  const { data } = useQuery({
    queryKey: searchKey(trimmed),
    queryFn: () => fetchSearch(trimmed),
    enabled: trimmed.length > 0,
  });

  return (
    <div data-testid="global-search-box">
      <input
        data-testid="global-search"
        type="search"
        aria-label="Search clients and projects"
        placeholder="Search clients and projects"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      {trimmed.length > 0 && data ? (
        <div data-testid="search-results">
          <div data-testid="search-clients">
            {data.clients.map((client) => (
              <div key={client.id} data-testid={`client-row-${client.id}`}>
                <span data-testid="client-name">{client.name}</span>
              </div>
            ))}
          </div>
          <div data-testid="search-projects">
            {data.projects.map((project) => (
              <div key={project.id} data-testid={`project-row-${project.id}`}>
                <span data-testid="project-name">{project.name}</span>
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

export const contribution = AppNav.fill({ order: 100, Component: GlobalSearch });
