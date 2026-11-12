import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Person, usePeople } from '../people/people';
import { usePersonDue } from '../rates/due.slot';
import { type Crew, crewDue, crewsKey, useCrews } from './crews';
import { useFigure } from '../locale/locale';

const refusal = (e: unknown, fallback: string) =>
  e instanceof HttpError && e.reason ? `${e.reason}.` : fallback;

// One crew: its lead, its members, its due-total (the lead's and the members' due-totals added up, skipping
// n/a), and the entry a member is added with. The lead, or a person already in the crew, is refused with an
// error on the crew.
function CrewRow({ crew, people, due }: { crew: Crew; people: Person[]; due: (p: Person) => string }) {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [personId, setPersonId] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (id: number | null) =>
      postJson(`/api/sheets/${encodeURIComponent(sheet)}/crews/${crew.id}/members`, { personId: id, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
      setPersonId('');
    },
  });
  const submit = useSubmitOnce(add);

  const total = crewDue(crew, people, due);

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong> — lead <span data-testid="crew-lead">{crew.lead ?? ''}</span>, due total{' '}
      <span data-testid="crew-due-total">{fig(total)}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            {m.name}
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(personId === '' ? null : Number(personId));
        }}
      >
        <select
          data-testid="crew-member-select"
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
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="crew-error" role="alert">
            {' '}
            {refusal(add.error, 'The member was refused.')}
          </span>
        )}
      </form>
    </li>
  );
}

// The sheet's crews, and the form a crew is added with: a name and a lead. A blank name, or one already a
// crew, is refused.
function Crews() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const crews = useCrews(sheet);
  const people = usePeople(sheet).data ?? [];
  const due = usePersonDue(sheet);
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const queryClient = useQueryClient();

  const add = useMutation({
    mutationFn: (c: { name: string; leadId: number | null }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheet)}/crews`, { ...c, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
      setName('');
      setLeadId('');
    },
  });
  const submit = useSubmitOnce(add);

  return (
    <section data-testid="crews">
      <h2>Crews</h2>
      <ul>
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.id} crew={c} people={people} due={due} />
        ))}
      </ul>
      <form
        data-testid="crew-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, leadId: leadId === '' ? null : Number(leadId) });
        }}
      >
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />
        <select
          data-testid="crew-form-lead"
          value={leadId}
          onChange={(e) => {
            setLeadId(e.target.value);
            add.reset();
          }}
        >
          <option value="">Lead…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {add.isError && (
          <span data-testid="crew-form-error" role="alert">
            {' '}
            {refusal(add.error, 'The crew was refused.')}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47.9, Component: Crews });
