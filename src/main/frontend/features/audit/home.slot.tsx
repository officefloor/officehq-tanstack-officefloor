import { HomeMain } from '../../slots/defs/homeMain';
import { AuditViewer } from './AuditViewer';

export const contribution = HomeMain.fill({ order: 60, Component: AuditViewer });
