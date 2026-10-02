import { Link } from '@tanstack/react-router';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSearchData } from './search';

// One search box that looks across BOTH clients and projects. It OWNS the `q` URL param — a search
// outlives a click, so it lives in the URL, not a parent's useState (CLAUDE.md rule 4). The matches
// are read from the shared /api/search collection and filtered by name here, grouped by kind.
// Archived rows are tucked away, mirroring the per-section lists.
export function GlobalSearch() {
  const [query, setQuery] = useSearchParam('q', asString);
  const { data } = useSearchData();

  const needle = query.trim().toLowerCase();
  const clients = (data?.clients ?? []).filter(
    (c) => !c.archived && needle !== '' && c.name.toLowerCase().includes(needle),
  );
  const projects = (data?.projects ?? []).filter(
    (p) => !p.archived && needle !== '' && p.name.toLowerCase().includes(needle),
  );

  return (
    <section>
      <input
        data-testid="global-search"
        type="search"
        placeholder="Search clients and projects"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />

      <table data-testid="search-clients">
        <thead>
          <tr>
            <th>Client</th>
            <th>Email</th>
          </tr>
        </thead>
        <tbody>
          {clients.map((client) => (
            <tr key={client.id} data-testid={`client-row-${client.id}`}>
              <td data-testid="client-name">
                <Link to="/clients/$clientId" params={{ clientId: String(client.id) }}>
                  {client.name}
                </Link>
              </td>
              <td data-testid="client-email">{client.email}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <table data-testid="search-projects">
        <thead>
          <tr>
            <th>Project</th>
            <th>Client</th>
          </tr>
        </thead>
        <tbody>
          {projects.map((project) => (
            <tr key={project.id} data-testid={`project-row-${project.id}`}>
              <td data-testid="project-name">
                <Link to="/projects/$projectId" params={{ projectId: String(project.id) }}>
                  {project.name}
                </Link>
              </td>
              <td data-testid="project-client">{project.clientName}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
