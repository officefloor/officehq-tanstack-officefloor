import { ClaimRowValues } from '../../slots/defs/claimRow';
import { HomeMain } from '../../slots/defs/homeMain';
import { ClaimTags } from './ClaimTags';
import { TagTotals } from './TagTotals';

export const contributions = [
  ClaimRowValues.fill({ order: 11.5, Component: ClaimTags }),
  HomeMain.fill({ order: 41, Component: TagTotals }),
];
