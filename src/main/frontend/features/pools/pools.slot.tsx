import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { usePeople } from '../people/people';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { poolMembersPath, poolsKey, poolsPath, usePools, type Pool } from './pools';

// One pool's row: its members with their shares, its total share and whether it is balanced, and a form that adds
// a member chosen from the sheet's people.
function PoolRow({ sheet, user, pool }: { sheet: string; user: string; pool: Pool }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const [person, setPerson] = useState('');
  const [share, setShare] = useState('');

  const add = useMutation({
    mutationFn: (body: { pool: string; person: number | null; share: string }) =>
      postJson<Pool[]>(byUser(poolMembersPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(poolsKey(sheet), updated);
      setPerson('');
      setShare('');
    },
  });
  const send = useSubmitOnce(add);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a person already in the pool or a bad share.
    send({ pool: pool.name, person: person === '' ? null : Number(person), share });
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total share <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.person} data-testid={`pool-member-${pool.name}-${m.person}`}>
            {m.name} <span data-testid="pool-share">{m.share}</span>%{m.retired ? ' (retired)' : ''}
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="pool-member-select"
          value={person}
          onChange={(e) => {
            setPerson(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <label>
          Share %{' '}
          <input
            data-testid="pool-member-share"
            value={share}
            onChange={(e) => {
              setShare(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error" role="alert">
            {' '}A member must not already be in the pool, and a share has two decimals, above 0.00 and at most 100.00.
          </span>
        )}
      </form>
    </li>
  );
}

// The sheet's pools, and a form that adds one.
function Pools({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: pools = [] } = usePools(sheet);
  const [name, setName] = useState('');

  const add = useMutation({
    mutationFn: (body: { name: string }) => postJson<Pool[]>(byUser(poolsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(poolsKey(sheet), updated);
      setName('');
    },
  });
  const send = useSubmitOnce(add);

  const change = (next: string) => {
    setName(next);
    add.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a blank name or one already a pool.
    send({ name });
  };

  return (
    <section>
      <h2>Pools</h2>
      <form onSubmit={submit}>
        <label>
          Name <input data-testid="pool-form-name" value={name} onChange={(e) => change(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && (
          <span data-testid="pool-form-error" role="alert">
            {' '}A pool's name must not be blank nor already a pool.
          </span>
        )}
      </form>
      <ul>
        {pools.map((p) => (
          <PoolRow key={p.name} sheet={sheet} user={user} pool={p} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.8, Component: Pools });
