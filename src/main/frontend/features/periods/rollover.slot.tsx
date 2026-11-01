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
  const clear = () => {
    rolling.reset();
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
        </button>
        {rolling.isSuccess && (
          <span>
            {' '}
            Made <strong data-testid="rollover-made">{rolling.data.made}</strong>
          </span>
        )}
        {(invalid !== null || rolling.isError) && (
          <span data-testid="rollover-error" role="alert">
            {' '}
            {invalid ??
              (rolling.error instanceof HttpError && rolling.error.body
                ? rolling.error.body
                : 'Could not roll this year over.')}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6.6, Component: Rollover });
