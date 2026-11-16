import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { usePeople } from '../people/people';
import { poolsKey, usePools, type Pool, type PoolMember } from './pools';

/** One pool: its total share and whether it is balanced, each member's line with their share, and the picker that adds
 *  a person with a share. A person already in the pool, or a share not more than 0.00 and at most 100.00 with two
 *  decimals, is refused with the pool's error. */
function PoolRow({ sheetId, user, pool }: { sheetId: string; user: string; pool: Pool }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');
  const [share, setShare] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (m: { personId: number; share: string }) =>
      postJson<PoolMember>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, { pool: pool.name, ...m, by: user }),
    onSuccess: async () => {
      setChosen('');
      setShare('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total <span data-testid="pool-total-share">{pool.totalShare.toFixed(2)}</span>, balanced{' '}
      <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            {m.name}: <span data-testid="pool-share">{m.share.toFixed(2)}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <select
        data-testid="pool-member-select"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          add.reset();
        }}
      >
        <option value="">Person…</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <input
        data-testid="pool-member-share"
        placeholder="Share %"
        value={share}
        onChange={(e) => {
          setShare(e.target.value);
          add.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="pool-member-add"
        disabled={add.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          add.reset();
          add.mutate({ personId: Number(chosen), share }); // nothing chosen: no person 0, so the server refuses it
        }}
      >
        Add member
      </button>
      {add.isError && <span data-testid="pool-error"> Could not add this member.</span>}
    </li>
  );
}

/** The sheet's pools: the form that adds one (its name trimmed, not blank, not already taken), and each pool's row. */
function Pools({ sheetId, user }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (n: string) => postJson<Pool>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, { name: n, by: user }),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <section data-testid="pools">
      <h3>Pools</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          add.reset();
          add.mutate(name);
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
        />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && <span data-testid="pool-form-error"> A pool's name must not be blank nor already taken.</span>}
      </form>
      {pools.isError && <p data-testid="pools-load-error">Could not load pools.</p>}
      <ul>
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.name} sheetId={sheetId} user={user} pool={p} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 9.7, Component: Pools });
