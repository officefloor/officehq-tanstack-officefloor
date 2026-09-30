import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectTags } from './ProjectTags';

// The labels panel on a project's detail page — one new *.slot.tsx file filling the ProjectDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows the project's tags and lets
// them be added/removed; the panel queries/mutates for itself under the shared key.
function ProjectTagsPanel({ projectId }: { projectId: number }) {
  return <ProjectTags projectId={projectId} />;
}

export const contribution = ProjectDetail.fill({ order: 20, Component: ProjectTagsPanel });
