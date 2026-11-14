import { AppHeader } from '../../slots/defs/appHeader';
import { ClaimRowValues } from '../../slots/defs/claimRow';
import { ClaimPolicy } from './ClaimPolicy';
import { PolicyVersion } from './PolicyVersion';
import { PolicyVersions } from './PolicyVersions';

export const contributions = [
  AppHeader.fill({ order: 27, Component: PolicyVersion }),
  AppHeader.fill({ order: 27.5, Component: PolicyVersions }),
  ClaimRowValues.fill({ order: 10.5, Component: ClaimPolicy }),
];
