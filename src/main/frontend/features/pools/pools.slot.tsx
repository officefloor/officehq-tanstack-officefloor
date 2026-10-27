import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { poolsKey, poolsQuery } from './poolsQuery';
import type { Pool } from './poolsQuery';

/** One pool's row: its total share and whether it is balanced, its members with their shares, and the form a member is
 *  added through. A person already in the pool, or a bad share, is refused with an error on the pool. */
function PoolRow({ sheetId, pool, people }: { sheetId: string; pool: Pool; people: Person[] }) {
  const queryClient = useQueryClient();
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { pool: string; personId: number | null; share: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pool-members`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setPersonId('');
      setShare('');
    },
    onError: (e: Error) => setError(e instanceof RefusedError ? e.message : 'The member could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ pool: pool.name, personId: personId === '' ? null : Number(personId), share: share.trim() }));
  };

  const edit = (set: (v: string) => void) => (v: string) => {
    set(v);
    setError('');
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong data-testid="pool-name">{pool.name}</strong> — total <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            <span data-testid="pool-member-name">{m.name}</span> <span data-testid="pool-share">{m.share}</span>
            {' '}cut <span data-testid="pool-cut">{m.cut}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select data-testid="pool-member-select" value={personId} onChange={(e) => edit(setPersonId)(e.target.value)}>
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <input data-testid="pool-member-share" placeholder="Share %" value={share} onChange={(e) => edit(setShare)(e.target.value)} />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && <p data-testid="pool-error">{error}</p>}
      </form>
    </li>
  );
}

/** The sheet's pools, and the form a pool is added through. A blank name, or one already a pool, is refused. */
function Pools({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const pools = useQuery(poolsQuery(sheetId));
  const people = useQuery(peopleQuery(sheetId));
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string }) => postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setName('');
    },
    onError: (e: Error) => setError(e instanceof RefusedError ? e.message : 'The pool could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ name: name.trim() }));
  };

  return (
    <section data-testid="pools">
      <h3>Pools</h3>
      <form onSubmit={submit}>
        <input
          data-testid="pool-form-name"
          placeholder="Pool name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {error && <p data-testid="pool-form-error">{error}</p>}
      </form>
      {pools.isError && <p data-testid="pools-load-error">Could not load the pools.</p>}
      <ul>
        {(pools.data ?? []).map((p) => (
          <PoolRow key={p.name} sheetId={sheetId} pool={p} people={people.data ?? []} />
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.8,
  Component: ({ sheetId }) => <Pools key={`pools-${sheetId}`} sheetId={sheetId} />,
});
