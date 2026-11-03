import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople, type Person } from '../people/usePeople';
import { poolsKey, usePools, type Pool } from './usePools';

/** One pool: its members with their shares, its total share and whether it is balanced, and a form that
 *  adds a member (a person on the sheet and a share percent). */
function PoolRow({ sheetId, pool, people }: { sheetId: string; pool: Pool; people: Person[] }) {
  const queryClient = useQueryClient();
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');

  const submitOnce = useSingleFlight();
  const add = useMutation({
    mutationFn: (f: { pool: string; personId: number | null; share: string }) =>
      postJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools/members`, f),
    onSuccess: async () => {
      setPersonId('');
      setShare('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  return (
    <div data-testid={`pool-row-${pool.name}`}>
      <strong data-testid="pool-name">{pool.name}</strong>{' '}
      Total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      Balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
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
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ pool: pool.name, personId: personId === '' ? null : Number(personId), share });
          });
        }}
      >
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            setPersonId(e.target.value);
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
          Share{' '}
          <input
            data-testid="pool-member-share"
            inputMode="decimal"
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
      </form>
      {add.isError && <p data-testid="pool-error">{add.error.message}</p>}
    </div>
  );
}

/** The sheet's pools: a form that adds one (a name, unique on the sheet) and each pool with its members. */
function Pools({ sheetId }: { sheetId: string }) {
  const pools = usePools(sheetId);
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string }) =>
      postJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, f),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
    },
  });

  return (
    <section data-testid="pools">
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name });
          });
        }}
      >
        <label>
          Pool{' '}
          <input
            data-testid="pool-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={save.isPending}>
          Add pool
        </button>
        {save.isError && <p data-testid="pool-form-error">{save.error.message}</p>}
      </form>
      {(pools.data ?? []).map((p) => (
        <PoolRow key={p.name} sheetId={sheetId} pool={p} people={people.data ?? []} />
      ))}
      {pools.isError && <p data-testid="pools-error">{pools.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7, Component: ({ sheetId }) => <Pools sheetId={sheetId} /> });
