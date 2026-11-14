import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ExportPerson } from './ExportPerson';

export const contributions = [
  PersonColumnHeads.fill({ order: 99, Component: () => <th>Export</th> }),
  PersonColumns.fill({ order: 99, Component: ExportPerson }),
];
