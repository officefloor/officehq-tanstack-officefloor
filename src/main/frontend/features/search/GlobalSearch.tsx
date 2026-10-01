import { useQuery } from '@tanstack/react-query';
import { useSearchParam, asString } from '../../url/useSearchParam';
import {
  clientsKey,
  projectsKey,
  listClients,
  listProjects,
  type SearchClient,
  type SearchProject,
} from './api';

// One search box that looks across BOTH clients and projects. The committed term lives in the URL
// under 'q' (not useState — it outlives the click and is shareable/back-button friendly), so the box
// owns the key and the results read the same key. Each kind queries for itself under its shared key
// (['clients'], ['projects']) — nothing is imported from those features. Matching is a
// case-insensitive substring on the name; an empty box shows nothing until the user types.
export function GlobalSearch() {
  const [query, setQuery] = useSearchParam('q', asString);
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClients });
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: listProjects });

  const needle = query.trim().toLowerCase();
  const clientMatches = needle
    ? (clients ?? []).filter((c: SearchClient) => c.name.toLowerCase().includes(needle))
    : [];
  const projectMatches = needle
    ? (projects ?? []).filter((p: SearchProject) => p.name.toLowerCase().includes(needle))
    : [];

  return (
    <section data-testid="global-search-panel">
      <input
        data-testid="global-search"
        type="search"
        placeholder="Search clients and jobs"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <div data-testid="search-clients">
        <h2>Clients</h2>
        {clientMatches.map((c: SearchClient) => (
          <div key={c.id} data-testid={`client-row-${c.id}`}>
            <span data-testid="client-name">{c.name}</span>
          </div>
        ))}
      </div>
      <div data-testid="search-projects">
        <h2>Jobs</h2>
        {projectMatches.map((p: SearchProject) => (
          <div key={p.id} data-testid={`project-row-${p.id}`}>
            <span data-testid="project-name">{p.name}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
