import { SheetBody } from '../../slots/defs/sheetBody';
import { ActivityViewer } from './ActivityViewer';
import { AuditViewer } from './AuditViewer';

export const contributions = [
  SheetBody.fill({ order: 90, Component: AuditViewer }),
  SheetBody.fill({ order: 91, Component: ActivityViewer }),
];
