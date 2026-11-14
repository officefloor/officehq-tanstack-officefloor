import type { ReactNode } from 'react';
import { useColumnShown } from './columns';
import type { Column } from './columns';

/** The people list's header cell of a column the user may switch off: none while it is off. */
export function ColumnHead({ column, children }: { column: Column; children: ReactNode }) {
  return useColumnShown(column) ? <th>{children}</th> : null;
}
