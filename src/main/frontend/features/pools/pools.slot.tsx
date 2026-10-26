import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { peopleKey, usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';

/** A pool member: a person (their id), or another pool (personId null, `pool` its name); its name, share (two
 *  decimals), whether the person is retired, and its cut of the pool's pot (two decimals; null while the pool is
 *  unbalanced or has no pot — a member pool's pot is its cut in the pool above, a top pool's the sheet's pot). */
export type PoolMember = {
  personId: number | null;
  pool: string | null;
  name: string;
  share: string;
  retired: boolean;
  cut: string | null;
};

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

/** The member picked: `person:<id>` or `pool:<name>`. */
type MemberBody = { member: string; share: string };

// A pool's row: its total share and balanced yes/no, each member's share and cut of the pot on their line, and a picker
// of a person or another pool (pool:<name>) with a share that adds one more member. A person already in the pool, a
// pool that is already a member of a pool, would contain itself or make a chain more than three deep, or a share that is not more than 0.00 and at most
// 100.00 with at most two decimals, is refused with an error on the pool until its fields are changed or it is
// submitted again.
function PoolRow({ sheetId, pool, pools }: { sheetId: string; pool: Pool; pools: Pool[] }) {
  const people = usePeople(sheetId).data ?? [];
  const [member, setMember] = useState('');
  const [share, setShare] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (body: MemberBody) => {
      if (body.member === '') throw new Error('Choose a person or a pool');
      const memberPool = body.member.startsWith('pool:') ? body.member.slice('pool:'.length) : null;
      // `by` is the signed-in user the change is recorded against.
      return postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, {
        pool: pool.name,
        person: memberPool === null ? Number(body.member.slice('person:'.length)) : null,
        memberPool,
        share: body.share,
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setMember('');
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
          <li
            key={m.pool === null ? `person-${m.personId}` : `pool-${m.pool}`}
            data-testid={`pool-member-${pool.name}-${m.pool === null ? m.personId : `pool-${m.pool}`}`}
          >
            <span data-testid="pool-member-name">{m.name}</span>{' '}
            <span data-testid="pool-share">{m.share}</span>{' '}
            cut <span data-testid="pool-cut">{m.cut ?? ''}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          add.reset();
          submit({ member, share });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={member}
          onChange={(e) => {
            setMember(e.target.value);
            add.reset();
          }}
        >
          <option value="">Person or pool…</option>
          {people.map((p) => (
            <option key={p.id} value={`person:${p.id}`}>
              {p.name}
            </option>
          ))}
          {pools.map((p) => (
            <option key={`pool:${p.name}`} value={`pool:${p.name}`}>
              {`pool:${p.name}`}
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
          <PoolRow key={p.name} sheetId={sheetId} pool={p} pools={pools.data ?? []} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8.7, Component: PoolsPanel });
