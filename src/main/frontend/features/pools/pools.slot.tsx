import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { usePeople } from '../people/people';
import { type Pool, poolsKey, usePools } from './pools';

const refusal = (e: unknown, fallback: string) =>
  e instanceof HttpError && e.reason ? `${e.reason}.` : fallback;

// One pool: its members with their shares, its total share and whether it is balanced, and the entry a
// member is added with. A person already in the pool, or a bad share, is refused with an error on the pool.
function PoolRow({ pool }: { pool: Pool }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet).data ?? [];
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (m: { personId: number | null; share: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheet)}/pools/${pool.id}/members`, { ...m, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setPersonId('');
      setShare('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total <span data-testid="pool-total-share">{pool.totalShare}</span>, balanced{' '}
      <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            {m.name}: <span data-testid="pool-share">{m.share}</span>%{m.retired ? ' (retired)' : ''}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit({ personId: personId === '' ? null : Number(personId), share });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            add.reset();
          }}
        >
          <option value="">Person…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>
        <input
          data-testid="pool-member-share"
          placeholder="Share %"
          value={share}
          onChange={(e) => {
            setShare(e.target.value);
            add.reset();
          }}
        />
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error" role="alert">
            {' '}
            {refusal(add.error, 'The member was refused.')}
          </span>
        )}
      </form>
    </li>
  );
}

// The sheet's pools, and the form a pool is added with. A blank name, or one already a pool, is refused.
function Pools() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const pools = usePools(sheet);
  const [name, setName] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (n: string) => postJson(`/api/sheets/${encodeURIComponent(sheet)}/pools`, { name: n, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setName('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <ul>
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.id} pool={p} />
        ))}
      </ul>
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(name);
        }}
      >
        <input
          data-testid="pool-form-name"
          placeholder="Pool name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && (
          <span data-testid="pool-form-error" role="alert">
            {' '}
            {refusal(add.error, 'The pool was refused.')}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47.7, Component: Pools });
