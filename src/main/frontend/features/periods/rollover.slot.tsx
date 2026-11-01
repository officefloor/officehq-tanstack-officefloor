import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { periodsKey, YEAR } from './usePeriods';

/**
 * A steward rolls a fiscal year (yyyy) over: the next fiscal year's twelve periods are generated as
 * the fiscal year generator makes them, and each person's open figure at the end of the old year's
 * last period is added to their carried figure for the new year's first period. A year with no
 * periods, anyone but a steward, or a bad year is refused. Shows how many periods were made; the
 * count goes while the next press runs.
 *
 * The steward who made the last rollover can undo it on the sheet's today it was made: the periods
 * it generated go and the opening balances it set are taken back. Anyone else, a later day, or a
 * generated period a claim belongs to is refused.
 */
function Rollover({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const [invalid, setInvalid] = useState<string | null>(null);
  const rolling = useMutation({
    mutationFn: (y: string) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheet)}/rollover`, { year: y, user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const undoing = useMutation({
    mutationFn: () => postJson<{ fy: number; removed: number }>(
      `/api/sheets/${encodeURIComponent(sheet)}/rollover-undo`, { user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const failed = rolling.isError ? rolling.error : undoing.isError ? undoing.error : null;
  const clear = () => {
    rolling.reset();
    undoing.reset();
    setInvalid(null);
  };
  return (
    <section data-testid="rollover-panel">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          clear();
          const y = year.trim();
          if (!YEAR.test(y)) {
            setInvalid('The fiscal year must be a year, yyyy.');
            return;
          }
          rolling.mutate(y);
        }}
      >
        <label>
          Roll over fiscal year{' '}
          <input
            data-testid="rollover-year"
            placeholder="yyyy"
            size={6}
            value={year}
            onChange={(e) => {
              setYear(e.target.value);
              clear();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rollover" disabled={rolling.isPending}>
          Roll over
        </button>{' '}
        {rolling.isSuccess && (
          <span>
            {' '}
            Made <strong data-testid="rollover-made">{rolling.data.made}</strong>
          </span>
        )}
        <button
          type="button"
          data-testid="rollover-undo"
          disabled={undoing.isPending}
          onClick={() => {
            clear();
            undoing.mutate();
          }}
        >
          Undo last rollover
        </button>
        {(invalid !== null || failed !== null) && (
          <span data-testid="rollover-error" role="alert">
            {' '}
            {invalid ??
              (failed instanceof HttpError && failed.body
                ? failed.body
                : rolling.isError
                  ? 'Could not roll this year over.'
                  : 'Could not undo the last rollover.')}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6.6, Component: Rollover });
