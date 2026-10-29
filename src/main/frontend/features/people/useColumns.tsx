import type { ReactNode } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { asString, useSearchParam } from '../../url/useSearchParam';

const KEY = 'hiddenColumns';
const parse = (raw: unknown) => new Set(asString(raw).split(',').filter((c) => c !== ''));

/** The people-list columns a person can switch off on their own screen. */
export const COLUMNS = [
  'claimed',
  'due-total',
  'excess',
  'statement-count',
  'stated-total',
  'statement-date',
  'overdue-count',
  'overdue-total',
  'patrons',
  'backing',
  'hold',
  'retired',
  'terms',
  'contact',
  'aliases',
] as const;
export type Column = (typeof COLUMNS)[number];

/**
 * The columns switched off, kept in the URL: all shown when the screen is first opened, and left as they
 * are by loading a sheet. Only what this screen shows changes — no total or other figure.
 */
export function useHiddenColumns(): [Set<string>, (column: Column, shown: boolean) => void] {
  const [hidden] = useSearchParam(KEY, parse);
  const navigate = useNavigate();
  // Worked out from the URL as it is when applied, so quick clicks on several boxes never undo each other.
  const set = (column: Column, shown: boolean) => {
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const next = parse(prev[KEY]);
        if (shown) next.delete(column);
        else next.add(column);
        const { [KEY]: _, ...rest } = prev;
        const value = COLUMNS.filter((c) => next.has(c)).join(',');
        return value === '' ? rest : { ...rest, [KEY]: value };
      },
      replace: true,
    });
  };
  return [hidden, set];
}

/** Whether every one of the columns is shown on this screen. */
export function useColumnsShown(...columns: Column[]): boolean {
  const [hidden] = useHiddenColumns();
  return columns.every((c) => !hidden.has(c));
}

/** Renders its children only while the column is shown: a column switched off has no cell at all. */
export function ColumnShown({ column, children }: { column: Column; children: ReactNode }) {
  return useColumnsShown(column) ? <>{children}</> : null;
}

/** The control a person uses to choose which columns their screen shows: a checkbox per column. */
export function ColumnChooser() {
  const [hidden, set] = useHiddenColumns();
  return (
    <fieldset>
      <legend>Columns</legend>
      {COLUMNS.map((c) => (
        <label key={c}>
          <input type="checkbox" data-testid={`column-${c}`} checked={!hidden.has(c)} onChange={(e) => set(c, e.target.checked)} />{' '}
          {c}{' '}
        </label>
      ))}
    </fieldset>
  );
}
