import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectNotesPanel } from './ProjectNotesPanel';

// The notes' presence on a project's detail page — its own file, filling the project-detail slot.
// The detail route was written once and is not touched to add this; the panel queries for its own
// data (['notes']) scoped to the project it is handed.
export const contribution = ProjectDetail.fill({
  order: 40,
  Component: ProjectNotesPanel,
});
