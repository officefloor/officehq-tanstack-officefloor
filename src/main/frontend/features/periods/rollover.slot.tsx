import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { openingsKey } from '../openings/openings';
import { periodsKey } from './periods';

type GenerateResult = { generated: number };
type UndoResult = { fy: string; removed: number };

// The year end rollover (a steward only): a fiscal year (yyyy) with periods on the sheet generates the next
// fiscal year's twelve months as the fiscal year generator does, and carries each person's open figure at the
// end of the old year into the new year's first period as an opening balance. The count of periods made shows
// once it answers (gone while it runs). The steward who made the last rollover may undo it on the sheet-today it
// was made, while no claim is dated in a period it generated; a refused rollover or undo shows the one error.
function Rollover() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [year, setYear] = useState('');
  const queryClient = useQueryClient();

  const rollover = useMutation({
    mutationFn: (y: string) =>
      postJson<GenerateResult>(`/api/sheets/${encodeURIComponent(sheet)}/periods/rollover`, { year: y, user }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: openingsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(rollover);

  const undo = useMutation({
    mutationFn: () =>
      postJson<UndoResult>(`/api/sheets/${encodeURIComponent(sheet)}/periods/rollover-undo`, { user }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
      void queryClient.invalidateQueries({ queryKey: openingsKey(sheet) });
    },
  });
  const submitUndo = useSubmitOnce(undo);
  const failed = rollover.isError ? rollover.error : undo.isError ? undo.error : null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        undo.reset();
        submit(year);
      }}
    >
      <h2>Year end rollover</h2>
      <input
        data-testid="rollover-year"
        placeholder="Fiscal year (yyyy)"
        value={year}
        onChange={(e) => {
          setYear(e.target.value);
          rollover.reset();
          undo.reset();
        }}
      />
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll year over
      </button>
      {rollover.isSuccess && (
        <>
          {' '}
          Made <strong data-testid="rollover-made">{rollover.data.generated}</strong>
        </>
      )}
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
        Undo last rollover
      </button>
      {failed && (
        <span data-testid="rollover-error" role="alert">
          {' '}
          {failed instanceof HttpError && failed.reason ? `${failed.reason}.` : 'The rollover was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = HomeMain.fill({ order: 46, Component: Rollover });
