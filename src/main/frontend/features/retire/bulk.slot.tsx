import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';
import { useSelectedPeople } from '../people/people';

type BulkRetireResult = { retired: number };

// Retires the selected people together from the date (the sheet's today when blank): the server
// retires each in list order as the row's retire would, and the count shows once it answers (gone
// while it runs). A refusal changes nothing.
function BulkRetire() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedPeople();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (p: { personIds: number[]; date: string }) =>
      postJson<BulkRetireResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-retire`, {
        personIds: p.personIds.map(String),
        date: p.date,
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      // Retirements move people's figures and the sheet's totals, so the whole sheet is re-read.
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      <input
        data-testid="bulk-retire-date"
        type="date"
        aria-label="Retire from (blank for the sheet's today)"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          bulk.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={bulk.isPending}
        onClick={() => submit({ personIds: selected, date })}
      >
        Retire selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Retired <strong data-testid="bulk-retired">{bulk.data.retired}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk retire was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 21.2, Component: BulkRetire });
