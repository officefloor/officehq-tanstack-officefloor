import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { periodsKey } from '../periods/usePeriods';

/** Year-end rollover (a steward only): a fiscal year (yyyy) and its button generate the next fiscal year's twelve
 *  monthly periods as the fiscal year generator does, and carry each person's open figure at the end of the old
 *  year's last period into their opening balance for the new year's first period. Shows how many periods were made
 *  (gone while it runs); a bad year, a year with no periods, or anyone but a steward is refused with an error.
 *  Its undo button takes the last rollover back (its periods removed, its opening balances taken back) for the
 *  steward who made it on the same sheet-today; otherwise, or once a claim belongs to one of its periods, it is
 *  refused with the same error element. */
function Rollover({ sheetId }: { sheetId: string }) {
  const [year, setYear] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const rollover = useMutation({
    mutationFn: (y: string) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/rollover`, { year: y }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const undo = useMutation({
    mutationFn: () =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/rollover-undo`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  const error = rollover.error ?? undo.error;
  return (
    <form
      data-testid="rollover-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          rollover.reset();
          undo.reset();
          return rollover.mutateAsync(year);
        });
      }}
    >
      <label>
        Roll over fiscal year{' '}
        <input
          placeholder="yyyy"
          data-testid="rollover-year"
          value={year}
          onChange={(e) => {
            setYear(e.target.value);
            rollover.reset();
            undo.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
        Roll year over
      </button>
      {rollover.isSuccess && (
        <>
          {' '}
          Made <strong data-testid="rollover-made">{rollover.data.made}</strong>
        </>
      )}
      <button
        type="button"
        data-testid="rollover-undo"
        disabled={undo.isPending}
        onClick={() =>
          submitOnce(() => {
            rollover.reset();
            undo.reset();
            return undo.mutateAsync();
          })
        }
      >
        Undo last rollover
      </button>
      {error && <p data-testid="rollover-error">{error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({
  order: 11.6,
  Component: ({ sheetId }) => <Rollover sheetId={sheetId} />,
});
