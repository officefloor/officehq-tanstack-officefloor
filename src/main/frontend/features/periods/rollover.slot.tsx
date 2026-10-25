import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { periodsQueryKey, rolloverPath, rolloverUndoPath } from './periods';

// The year-end rollover (a steward's): the entry takes the old fiscal year (yyyy); the button generates the next
// fiscal year's twelve periods as the fiscal year generator does and adds each person's open figure at the end of the
// old year's last period to their carried figure for the new year's first. How many periods were made shows once
// done, and is gone while it runs. A bad year, a year with no periods, or anyone but a steward is refused.
// The undo button takes the last rollover back (its periods removed, its opening balances taken back) — only for the
// steward who made it, on the same sheet-today, and while no claim belongs to a period it generated.
function Rollover() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [year, setYear] = useState('');
  const queryClient = useQueryClient();
  const rollover = useMutation({
    mutationFn: (body: { year: string }) => postJson<{ made: number }>(rolloverPath(sheet ?? ''), body, user),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') });
    },
  });
  const undo = useMutation({
    mutationFn: () => postJson<{ undone: boolean }>(rolloverUndoPath(sheet ?? ''), {}, user),
    onSuccess: () => {
      void queryClient.invalidateQueries();
    },
  });
  const submit = useSubmitOnce(rollover);
  const submitUndo = useSubmitOnce(undo);
  if (!user || !sheet) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        undo.reset();
        submit({ year });
      }}
    >
      <h2>Year-end rollover</h2>
      <input
        data-testid="rollover-year"
        placeholder="Fiscal year (yyyy)"
        inputMode="numeric"
        value={year}
        onChange={(e) => { setYear(e.target.value); rollover.reset(); undo.reset(); }}
      />
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll the year over
      </button>
      {' '}
      <button
        type="button"
        data-testid="rollover-undo"
        disabled={undo.isPending}
        onClick={() => {
          rollover.reset();
          submitUndo(undefined);
        }}
      >
        Undo the last rollover
      </button>
      {rollover.isSuccess && (
        <>
          {' '}Made <span data-testid="rollover-made">{rollover.data.made}</span>
        </>
      )}
      {rollover.isError && (
        <span data-testid="rollover-error">
          Nothing was rolled over: only a steward can, and the fiscal year must be a year (yyyy) with periods.
        </span>
      )}
      {undo.isError && (
        <span data-testid="rollover-error">
          Nothing was undone: only the steward who made the last rollover can, on the same day, while no claim belongs to
          a period it generated.
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 16.5, Component: Rollover });
