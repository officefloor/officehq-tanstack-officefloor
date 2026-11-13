import { ClaimRowValues } from '../../slots/defs/claimRow';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ClaimTags } from './ClaimTags';
import { PersonTags, PersonTagsHead } from './PersonTags';
import { TagTotals } from './TagTotals';

export const contributions = [
  ClaimRowValues.fill({ order: 11.5, Component: ClaimTags }),
  PersonColumnHeads.fill({ order: 11.5, Component: PersonTagsHead }),
  PersonColumns.fill({ order: 11.5, Component: PersonTags }),
  HomeMain.fill({ order: 41, Component: TagTotals }),
];
