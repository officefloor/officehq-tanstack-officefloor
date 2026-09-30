import { ProjectDetail } from '../../slots/defs/projectDetail';
import { ProjectBudgetPanel } from './ProjectBudgetPanel';

// The budget's presence on a project's detail page — its own file, filling the project-detail slot
// above the invoices. The detail route was written once and is not touched to add this; the panel
// queries for its own data (a key starting ['invoices']) scoped to the project it is handed.
export const contribution = ProjectDetail.fill({
  order: 5,
  Component: ProjectBudgetPanel,
});
