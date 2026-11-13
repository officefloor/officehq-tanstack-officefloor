import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';
import { useSelectedPeople } from '../people/people';

type BulkHoldResult = { held: number };

// Holds the selected people together from the from-date (the sheet's today when blank) to the
// to-date (no end when blank): the server holds each in list order as the row's hold would, and the
// count shows once it answers (gone while it runs). A refusal changes nothing.
function BulkHold() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedPeople();
  const [fields, setFields] = useState({ from: '', to: '' });
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (p: { personIds: number[]; from: string; to: string }) =>
      postJson<BulkHoldResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-hold`, {
        personIds: p.personIds.map(String),
        from: p.from,
        to: p.to,
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      // Holds move people's figures, the sheet's totals and pools' cuts, so the whole sheet is re-read.
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  const set = (k: 'from' | 'to') => (e: ChangeEvent<HTMLInputElement>) => {
    setFields((f) => ({ ...f, [k]: e.target.value }));
    bulk.reset();
  };

  return (
    <p>
      <input
        data-testid="bulk-hold-from"
        type="date"
        aria-label="Hold from (blank for the sheet's today)"
        value={fields.from}
        onChange={set('from')}
      />{' '}
      <input
        data-testid="bulk-hold-to"
        type="date"
        aria-label="Hold to (blank for no end)"
        value={fields.to}
        onChange={set('to')}
      />{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={bulk.isPending}
        onClick={() => submit({ personIds: selected, ...fields })}
      >
        Hold selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Held <strong data-testid="bulk-held">{bulk.data.held}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk hold was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 21.3, Component: BulkHold });
