import { ProjectRow } from '../../slots/defs/projectRow';
import type { Project } from './projects';
import { useArchiveProject } from './projects';

// "Archive this project" — a self-contained row action filling the project-row region. Archiving
// tucks the project away rather than deleting it: nothing is lost, it just drops off the lists.
// Clicking archives it through the shared mutation, which invalidates ['projects'] so every view
// re-reads from the server and the row drops with no hand-maintained list. The server keeps the row
// and records the archiving to the audit file.
function ArchiveProject({ project }: { project: Project }) {
  const archive = useArchiveProject();
  return (
    <button
      type="button"
      data-testid={`project-archive-${project.id}`}
      disabled={archive.isPending}
      onClick={() => archive.mutate(project.id)}
    >
      Archive
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 20, Component: ArchiveProject });
