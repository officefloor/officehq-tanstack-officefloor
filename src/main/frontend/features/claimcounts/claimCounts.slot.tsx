import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonClaimCounts } from './PersonClaimCounts';

export const contributions = [
  PersonColumnHeads.fill({
    order: 11,
    Component: () => (
      <>
        <th>Open</th>
        <th>Settled</th>
      </>
    ),
  }),
  PersonColumns.fill({ order: 11, Component: PersonClaimCounts }),
];
