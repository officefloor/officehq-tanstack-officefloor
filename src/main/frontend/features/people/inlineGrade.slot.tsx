import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonInlineGrade } from './PersonInlineGrade';

export const contributions = [
  PersonColumnHeads.fill({ order: 0.5, Component: () => <th>Set grade</th> }),
  PersonColumns.fill({ order: 0.5, Component: PersonInlineGrade }),
];
