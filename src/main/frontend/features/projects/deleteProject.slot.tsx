import { ProjectRow } from '../../slots/defs/projectRow';
import type { Project } from './projects';
import { useDeleteProject } from './projects';

// "Delete this project" — a self-contained row action filling the project-row region. The projects
// table is not edited to add it (CLAUDE.md rule 3); it fills the slot the table renders per row.
// Clicking removes the project through the shared mutation, which invalidates ['projects'] so the
// list re-reads from the server and the row drops with no hand-maintained list. The server records
// the deletion to the audit file.
function DeleteProject({ project }: { project: Project }) {
  const remove = useDeleteProject();
  return (
    <button
      type="button"
      data-testid={`project-delete-${project.id}`}
      disabled={remove.isPending}
      onClick={() => remove.mutate(project.id)}
    >
      Delete
    </button>
  );
}

export const contribution = ProjectRow.fill({ order: 10, Component: DeleteProject });
