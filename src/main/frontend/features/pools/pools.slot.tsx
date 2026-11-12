import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { usePeople } from '../people/people';
import { type Pool, poolsKey, usePools } from './pools';
import { useFigure } from '../locale/locale';

const refusal = (e: unknown, fallback: string) =>
  e instanceof HttpError && e.reason ? `${e.reason}.` : fallback;

// One pool: its members with their shares and their cuts of the sheet's pot (blank when unbalanced, no pot or every member retired), its total share and whether it is balanced, and the entry a
// member is added with: a person, or another pool (pool:<name>). A person already in the pool, a pool already in
// a pool, in a loop or more than three deep, or a bad share, is refused with an error on the pool. A membership
// may carry a from- and a to-date (blank for no bound; a to-date before the from-date is refused): a member whose
// dates do not include the sheet's today counts in neither the total nor the balance, and has no cut.
function PoolRow({ pool, pools }: { pool: Pool; pools: Pool[] }) {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet).data ?? [];
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (m: { personId: number | null; memberPoolId: number | null; share: string; from: string; to: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheet)}/pools/${pool.id}/members`, { ...m, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setPersonId('');
      setShare('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total <span data-testid="pool-total-share">{fig(pool.totalShare)}</span>, balanced{' '}
      <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li
            key={m.memberPoolId !== null ? `pool-${m.memberPoolId}` : m.personId}
            data-testid={`pool-member-${pool.name}-${m.memberPoolId !== null ? `pool-${m.name}` : m.personId}`}
          >
            {m.name}: <span data-testid="pool-share">{fig(m.share)}</span>%{m.retired ? ' (retired)' : ''}
            {m.from !== null || m.to !== null ? ` (${m.from ?? '…'} – ${m.to ?? '…'})` : ''}, cut{' '}
            <span data-testid="pool-cut">{fig(m.cut ?? '')}</span>
            {m.periodCuts.map((c) => (
              <span key={c.period}>
                , {c.period}{' '}
                <span data-testid={`pool-cut-${c.period}`}>{fig(c.cut ?? '')}</span>
              </span>
            ))}
          </li>
        ))}
      </ul>
      {!pool.crew && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            submit({
              personId: personId === '' || personId.startsWith('pool:') ? null : Number(personId),
              memberPoolId: personId.startsWith('pool:') ? Number(personId.slice(5)) : null,
              share,
              from,
              to,
            });
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
            {pools.filter((p) => !p.crew).map((p) => (
              <option key={`pool:${p.id}`} value={`pool:${p.id}`}>
                pool:{p.name}
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
          <input
            type="date"
            data-testid="pool-member-from"
            aria-label="Member from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              add.reset();
            }}
          />
          <input
            type="date"
            data-testid="pool-member-to"
            aria-label="Member to"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
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
      )}
    </li>
  );
}

// The sheet's pools (each crew among them, after the others), and the form a pool is added with. A blank name, or
// one already a pool or a crew, is refused.
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
          <PoolRow key={p.id} pool={p} pools={pools.data ?? []} />
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
