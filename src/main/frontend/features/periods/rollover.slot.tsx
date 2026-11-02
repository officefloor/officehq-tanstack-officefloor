import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { periodsKey, rolloverPath, rolloverUndoPath } from './periods';
import type { Generated, Period } from './periods';

// The year end rollover: a steward enters a fiscal year (yyyy); the server generates the next fiscal year's twelve
// months exactly as the fiscal year generator does and adds each person's open figure at the end of the old year's
// last period to their carried figure for the new year's first period. It refuses anyone but a steward, a year that is
// not yyyy, or one with no periods. Shows how many periods were made. The steward who made the last rollover can undo
// it on the same sheet-today: its generated periods are removed and the openings it set taken back; the server refuses
// anyone else, a later day, or a generated period a claim belongs to.
function Rollover({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const rollover = useMutation({
    mutationFn: (body: string) => postJson<Generated>(byUser(rolloverPath(sheet), user), { year: body }),
    onSuccess: (result) => {
      queryClient.setQueryData(periodsKey(sheet), result.periods);
      setYear('');
    },
  });
  const send = useSubmitOnce(rollover);
  const undo = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(rolloverUndoPath(sheet), user), {}),
    onSuccess: (periods) => queryClient.setQueryData(periodsKey(sheet), periods),
  });
  const sendUndo = useSubmitOnce(undo);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    undo.reset();
    send(year);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Roll over fiscal year{' '}
        <input
          data-testid="rollover-year"
          placeholder="yyyy"
          value={year}
          onChange={(e) => {
            setYear(e.target.value);
            rollover.reset();
            undo.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll over year
      </button>{' '}
      <button
        type="button"
        data-testid="rollover-undo"
        disabled={undo.isPending}
        onClick={() => {
          rollover.reset();
          sendUndo(undefined);
        }}
      >
        Undo last rollover
      </button>
      {rollover.isSuccess && (
        <>
          {' '}Made <strong data-testid="rollover-made">{rollover.data.made}</strong>
        </>
      )}
      {rollover.isError && (
        <span data-testid="rollover-error" role="alert">
          {' '}Nothing was rolled over: only a steward may, of a fiscal year (yyyy) that has periods.
        </span>
      )}
      {undo.isError && (
        <span data-testid="rollover-error" role="alert">
          {' '}Nothing was undone: only the steward who made the last rollover may, on the same day, while no claim
          belongs to a period it made.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 25.6, Component: Rollover });
