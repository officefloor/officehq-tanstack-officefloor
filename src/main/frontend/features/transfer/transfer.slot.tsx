import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function TransferHead() {
  return <th>Transfer</th>;
}

function OriginHead() {
  return <th>From sheet</th>;
}

// The sheet this person was transferred from (empty for anyone never transferred).
function PersonOrigin({ person }: { sheetId: string; person: Person }) {
  return <td data-testid="person-origin">{person.origin}</td>;
}

// An entry for another sheet's id and a button that transfers this person there (a steward): they
// arrive with their grades, terms, contact, aliases and claims, and leave this sheet's list.
function PersonTransfer({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [to, setTo] = useState('');
  const [error, setError] = useRowError(person.id, 'transfer');
  const once = useOnce();
  const transfer = useMutation({
    mutationFn: (toSheet: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/transfer`,
        { toSheet },
        user,
      ),
    onSuccess: async (_person, toSheet) => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', toSheet.trim()] });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That person could not be transferred.'),
  });

  const submit = () => {
    setError('');
    once((done) => transfer.mutate(to, { onSettled: done }));
  };

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        value={to}
        placeholder="sheet id"
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="button" data-testid="person-transfer" onClick={submit} disabled={transfer.isPending}>
        Transfer
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

function TransferUndoHead() {
  return <th>Undo transfer</th>;
}

// A button that undoes the transfer that brought this person here (a steward, on the sheet's today
// they arrived): they go back to the sheet they came from, and leave this sheet's list.
function PersonTransferUndo({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useRowError(person.id, 'transfer-undo');
  const once = useOnce();
  const undo = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/transfer-undo`,
        {},
        user,
      ),
    onSuccess: async () => {
      if (person.origin) await queryClient.invalidateQueries({ queryKey: ['sheet', person.origin] });
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('That transfer could not be undone.'),
  });

  const submit = () => {
    setError('');
    once((done) => undo.mutate(undefined, { onSettled: done }));
  };

  return (
    <td>
      <button type="button" data-testid="person-transfer-undo" onClick={submit} disabled={undo.isPending}>
        Undo transfer
      </button>
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 64, Component: OriginHead }),
  PersonRow.fill({ order: 64, Component: PersonOrigin }),
  PersonHead.fill({ order: 66, Component: TransferHead }),
  PersonRow.fill({ order: 66, Component: PersonTransfer }),
  PersonHead.fill({ order: 66.5, Component: TransferUndoHead }),
  PersonRow.fill({ order: 66.5, Component: PersonTransferUndo }),
];
