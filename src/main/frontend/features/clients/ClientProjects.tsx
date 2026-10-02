import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The projects a client is doing work on, shown in the client's detail. Server data is read straight
// from the SHARED ['projects'] query key (CLAUDE.md rule 5) — the same key the Projects page uses —
// so creating a project anywhere refreshes this list too, with no import of the projects feature.
// Each project carries its clientId, so the client context is just a filter on that shared data.
type Project = {
  id: number;
  name: string;
  clientId: number;
  clientName: string;
  archived: boolean;
  status: string;
};

export function ClientProjects({ clientId }: { clientId: number }) {
  // Only the client's ACTIVE work shows by default; a toggle (owning the `clientProjectsShowAll`
  // URL key, CLAUDE.md rule 4) reveals the finished and hidden (archived) ones too. The flag lives
  // in the URL so the revealed view outlives the click and is shareable.
  const [showAll, setShowAll] = useSearchParam('clientProjectsShowAll', asFlag);
  const { data: projects } = useQuery({
    queryKey: ['projects'] as const,
    queryFn: () => getJson<Project[]>('/api/projects'),
  });

  if (!projects) {
    return null;
  }

  // Active = being worked on (status ACTIVE) and not tucked away (archived). By default only those
  // show; "Show all" widens to every project the client owns, finished and archived included.
  const owned = projects.filter((project) => {
    if (project.clientId !== clientId) {
      return false;
    }
    return showAll || (project.status === 'ACTIVE' && !project.archived);
  });

  return (
    <>
      <button
        type="button"
        data-testid="client-projects-show-all"
        aria-pressed={showAll}
        onClick={() => setShowAll(showAll ? undefined : true)}
      >
        {showAll ? 'Show active only' : 'Show all jobs'}
      </button>
      {owned.length === 0 ? (
        <p data-testid="client-projects-empty">No jobs for this client yet.</p>
      ) : (
        // Reuse the project-row-<id>/project-name anchors inside the client-context table.
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
      )}
    </>
  );
}
