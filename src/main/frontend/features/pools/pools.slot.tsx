import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { usePeople } from '../people/people';
import { poolsKey, usePools } from './pools';
import type { Pool } from './pools';

// The sheet's pools: a name entry and an add button, each pool listed below with its total share, whether
// it is balanced (the shares total exactly 100.00; a retired member's counts as 0.00) and its members, each
// with their share. A member is added from the pool's row: a person and a share. The server decides what is
// refused — a taken or blank name, a person already in the pool, a share not more than 0.00 and at most
// 100.00 with two decimals — and the form shows its error.
function Pools({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const pools = usePools(sheetId);
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { name: string }) =>
      postJson<Pool>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    onError: () => setError('That pool could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once((done) => add.mutate({ name }, { onSettled: done }));
  };

  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input
            data-testid="pool-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {error && (
          <p role="alert" data-testid="pool-form-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="pool-list">
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.name} sheetId={sheetId} pool={p} />
        ))}
      </ul>
    </section>
  );
}

function PoolRow({ sheetId, pool }: { sheetId: string; pool: Pool }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { pool: string; personId: string; share: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pool-members`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
    onError: () => setError(`That member could not be added to ${pool.name}.`),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once((done) => add.mutate({ pool: pool.name, personId, share }, { onSettled: done }));
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total share <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            {m.name}
            {m.retired ? ' (retired)' : ''}: <span data-testid="pool-share">{m.share}</span>%
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            setError('');
          }}
        >
          <option value="">Person…</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <input
          data-testid="pool-member-share"
          placeholder="share %"
          value={share}
          onChange={(e) => {
            setShare(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && (
          <p role="alert" data-testid="pool-error">
            {error}
          </p>
        )}
      </form>
    </li>
  );
}

export const contribution = SheetBody.fill({ order: -5.3, Component: Pools });
