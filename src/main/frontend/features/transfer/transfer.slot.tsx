import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import type { Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';

function OriginHead() {
  return <th>From sheet</th>;
}

/** The sheet the person was transferred from; empty for anyone never transferred here. */
function PersonOrigin({ person }: { sheetId: string; user: string; person: Person }) {
  return <td data-testid="person-origin">{person.origin ?? ''}</td>;
}

function TransferHead() {
  return <th>Transfer</th>;
}

/** The entry of the sheet id to transfer this person to, and the button that transfers them (a steward only). The
 *  person leaves this sheet with their claims, and their patron links and pool memberships are dropped, so every part of
 *  the sheet is re-read. On a sheet that already has one of their names they merge into that person. A transfer by
 *  anyone else, of a retired person, to this sheet, or to a sheet that already has their contact (when not merging),
 *  shows the row's error. */
function PersonTransfer({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [to, setTo] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'transfer');
  const transfer = useMutation({
    mutationFn: (toSheet: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/transfer`, { toSheet, by: user }),
    onSuccess: () => {
      setTo('');
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          transfer.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          rowAction.take();
          transfer.reset();
          transfer.mutate(to);
        }}
      >
        Transfer
      </button>
      {transfer.isError && rowAction.isLatest && <span data-testid="person-error"> Could not transfer this person.</span>}
    </td>
  );
}

function TransferUndoHead() {
  return <th>Undo transfer</th>;
}

/** The button that undoes the transfer that brought this person here (a steward only, on the sheet's today they
 *  arrived): they go back to the sheet they came from as a transfer takes them, so every part of the sheet is re-read.
 *  An undo by anyone else, on a later day, or of a person who did not arrive by a transfer shows the row's error. */
function PersonTransferUndo({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'transfer-undo');
  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/transfer-undo`, { by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] }),
    onSettled: submit.end,
  });

  return (
    <td>
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          rowAction.take();
          undo.reset();
          undo.mutate();
        }}
      >
        Undo transfer
      </button>
      {undo.isError && rowAction.isLatest && <span data-testid="person-error"> Could not undo this transfer.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 84, Component: OriginHead }),
  PersonRow.fill({ order: 84, Component: PersonOrigin }),
  PeopleHead.fill({ order: 85, Component: TransferHead }),
  PersonRow.fill({ order: 85, Component: PersonTransfer }),
  PeopleHead.fill({ order: 86, Component: TransferUndoHead }),
  PersonRow.fill({ order: 86, Component: PersonTransferUndo }),
];
