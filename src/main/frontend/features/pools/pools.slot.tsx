import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { peopleKey, usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';

/** A pool member: the person, their share (two decimals) and whether they are retired. */
export type PoolMember = { personId: number; name: string; share: string; retired: boolean };

/** A pool of the sheet: its name, members in the order added, total share (a retired member's counts as 0.00) and
 *  whether that total is exactly 100.00. */
export type Pool = { name: string; members: PoolMember[]; totalShare: string; balanced: boolean };

/** The key the sheet's pools are read under: below the people's key, so whatever changes the people (a retirement,
 *  a rename) re-reads the pools too. */
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'] as const;

/** The sheet's pools, in the order added. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
  });
}

type MemberBody = { person: string; share: string };

// A pool's row: its total share and balanced yes/no, each member's share on their line, and a person picker with a
// share that adds one more member. A person already in the pool, or a share that is not more than 0.00 and at most
// 100.00 with at most two decimals, is refused with an error on the pool until its fields are changed or it is
// submitted again.
function PoolRow({ sheetId, pool }: { sheetId: string; pool: Pool }) {
  const people = usePeople(sheetId).data ?? [];
  const [person, setPerson] = useState('');
  const [share, setShare] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (body: MemberBody) => {
      if (body.person === '') throw new Error('Choose a person');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, {
        pool: pool.name,
        person: Number(body.person),
        share: body.share,
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setPerson('');
      setShare('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <span data-testid="pool-name">{pool.name}</span>{' '}
      total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            <span data-testid="pool-member-name">{m.name}</span>{' '}
            <span data-testid="pool-share">{m.share}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
          submit({ person, share });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={person}
          onChange={(e) => {
            setPerson(e.target.value);
            add.reset();
          }}
        >
          <option value="">Person…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <input
          data-testid="pool-member-share"
          placeholder="Share %"
          inputMode="decimal"
          value={share}
          onChange={(e: ChangeEvent<HTMLInputElement>) => {
            setShare(e.target.value);
            add.reset();
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="pool-error" role="alert">
            {' '}
            {add.error.message}
          </span>
        )}
      </form>
    </li>
  );
}

// The pools: a form adds one by name (trimmed); a blank name or one already on the sheet is refused.
function PoolsPanel({ sheetId }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId);
  const [name, setName] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (value: string) => {
      if (value.trim() === '') throw new Error('Enter a pool name');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, { name: value, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setName('');
    },
  });

  const submit = useSubmitOnce(add);
  return (
    <section data-testid="pools-panel">
      <h2>Pools</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
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
        />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {add.isError && (
          <span data-testid="pool-form-error" role="alert">
            {' '}
            {add.error.message}
          </span>
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

export const contribution = SheetBody.fill({ order: 8.7, Component: PoolsPanel });
