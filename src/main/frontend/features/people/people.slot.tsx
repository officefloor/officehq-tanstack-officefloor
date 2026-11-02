import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { GRADES, peopleKey, usePeople, type Person } from './usePeople';

/** The sheet's people: a form that adds a person, or edits one when their editor is open, and the
 *  list of everyone in the order they were added. Each person is a <tbody> so features can add
 *  cells and rows of their own to it (slots/defs/personRow). */
function People({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');

  const save = useMutation({
    mutationFn: (f: { id: number | null; name: string; grade: string }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      return postJson<Person>(f.id === null ? base : `${base}/${f.id}`, { name: f.name, grade: f.grade });
    },
    onSuccess: async () => {
      setEditing(null);
      setName('');
      setGrade('A');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const openEditor = (p: Person) => {
    save.reset();
    setEditing(p.id);
    setName(p.name);
    setGrade(p.grade);
  };

  const cancel = () => {
    save.reset();
    setEditing(null);
    setName('');
    setGrade('A');
  };

  return (
    <section data-testid="people">
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ id: editing, name, grade });
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              save.reset();
            }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={cancel}>
              Cancel
            </button>
          </>
        )}
        {save.isError && <p data-testid="person-form-error">{save.error.message}</p>}
      </form>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PersonColumnHead.Slot sheetId={sheetId} />
            <th />
          </tr>
        </thead>
        {(people.data ?? []).map((p) => (
          <tbody key={p.id} data-testid={`person-row-${p.id}`}>
            <tr>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonCells.Slot sheetId={sheetId} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => openEditor(p)}>
                  Edit
                </button>
              </td>
            </tr>
            <PersonDetail.Slot sheetId={sheetId} personId={p.id} />
          </tbody>
        ))}
      </table>
      {people.isError && <p data-testid="people-error">{people.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 10,
  Component: ({ sheetId }) => <People sheetId={sheetId} />,
});
