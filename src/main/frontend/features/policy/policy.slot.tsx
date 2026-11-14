import { AppHeader } from '../../slots/defs/appHeader';
import { ClaimRowValues } from '../../slots/defs/claimRow';
import { ClaimPolicy } from './ClaimPolicy';
import { PolicyVersion } from './PolicyVersion';

export const contributions = [
  AppHeader.fill({ order: 27, Component: PolicyVersion }),
  ClaimRowValues.fill({ order: 10.5, Component: ClaimPolicy }),
];
