import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The projects a client is doing work on, shown in the client's detail. Server data is read straight
// from the SHARED ['projects'] query key (CLAUDE.md rule 5) — the same key the Projects page uses —
// so creating a project anywhere refreshes this list too, with no import of the projects feature.
// Each project carries its clientId, so the client context is just a filter on that shared data.
type Project = { id: number; name: string; clientId: number; clientName: string };

export function ClientProjects({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: ['projects'] as const,
    queryFn: () => getJson<Project[]>('/api/projects'),
  });

  if (!projects) {
    return null;
  }

  const owned = projects.filter((project) => project.clientId === clientId);

  if (owned.length === 0) {
    return <p data-testid="client-projects-empty">No projects for this client yet.</p>;
  }

  // Reuse the project-row-<id>/project-name anchors inside the client-context table.
  return (
    <table data-testid="client-projects-table">
      <thead>
        <tr>
          <th>Name</th>
        </tr>
      </thead>
      <tbody>
        {owned.map((project) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
