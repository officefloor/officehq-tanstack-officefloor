import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from './people';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { sheetKey } from '../sheet/sheet';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function OriginHead() {
  return <th>From sheet</th>;
}

// The sheet a transferred person came from; blank for anyone added on this sheet.
function PersonOrigin({ person }: { person: Person }) {
  return <td data-testid="person-origin">{person.origin ?? ''}</td>;
}

function TransferHead() {
  return <th>Transfer</th>;
}

// The id of the sheet to transfer this person to, and the button that transfers them: a steward's
// action. The person, with their grades, terms, contact, aliases and claims, moves to the other sheet
// and their row leaves this one. A refused transfer shows its error until the next action is taken.
function PersonTransfer({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const transfer = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (toSheet: string) =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer`, { to: toSheet, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: [...sheetKey(sheet), 'watches'] }),
      ]);
      setTo('');
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={to}
        onChange={(e) => {
          transfer.reset();
          setTo(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          transfer.reset();
          transfer.mutate(to);
        }}
      >
        Transfer
      </button>
      {transfer.isError && transfer.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {transfer.error instanceof HttpError && transfer.error.reason
            ? `${transfer.error.reason}.`
            : 'The transfer was refused.'}
        </span>
      )}
    </td>
  );
}

function TransferUndoHead() {
  return <th>Undo transfer</th>;
}

// The button that undoes the transfer that brought this person here: a steward's action, on the sheet-today they
// arrived. They are transferred back to the sheet they came from and their row leaves this one. A refused undo
// shows its error until the next action is taken.
function PersonTransferUndo({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  const undo = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer-undo`, { user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: [...sheetKey(sheet), 'watches'] }),
      ]);
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => {
          undo.reset();
          undo.mutate();
        }}
      >
        Undo transfer
      </button>
      {undo.isError && undo.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {undo.error instanceof HttpError && undo.error.reason
            ? `${undo.error.reason}.`
            : 'The transfer undo was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.94, Component: OriginHead }),
  PersonColumns.fill({ order: 19.94, Component: PersonOrigin }),
  PersonColumnHeads.fill({ order: 19.95, Component: TransferHead }),
  PersonColumns.fill({ order: 19.95, Component: PersonTransfer }),
  PersonColumnHeads.fill({ order: 19.955, Component: TransferUndoHead }),
  PersonColumns.fill({ order: 19.955, Component: PersonTransferUndo }),
];
