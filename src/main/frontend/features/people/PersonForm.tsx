import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, peopleKey, usePeople, type Person } from './usePeople';

// Adds a person, or — while a person's editor is open (`editPerson` in the URL) — saves that
// person. Opening the editor fills the form with the person's values.
export function PersonForm() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editParam, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const edited = people.find((p) => p.id === editParam);
  // A person not on this sheet (an id left from another sheet) has no editor open.
  const editing = edited?.id ?? '';
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (body: { name: string; grade: string }) =>
      editing !== ''
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(editing)}`, { ...body, user })
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people`, { ...body, user }),
    onSuccess: () => {
      setName('');
      setGrade('A');
      if (editing !== '') setEditing(undefined);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  // Opening an editor fills the form with that person's values.
  const editedId = edited?.id;
  useEffect(() => {
    save.reset();
    if (edited) {
      setName(edited.name);
      setGrade(edited.grade);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editedId]);

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        save.mutate({ name, grade });
      }}
    >
      <input
        data-testid="person-form-name"
        value={name}
        placeholder="Name"
        onChange={(e) => {
          setName(e.target.value);
          save.reset();
        }}
      />{' '}
      <select
        data-testid="person-form-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          save.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button type="submit" data-testid="person-form-submit">{editing !== '' ? 'Save' : 'Add person'}</button>
      {editing !== '' && (
        <button
          type="button"
          data-testid="person-form-cancel"
          onClick={() => {
            setEditing(undefined);
            setName('');
            setGrade('A');
            save.reset();
          }}
        >
          Cancel
        </button>
      )}
      {save.isError && <span data-testid="person-form-error" role="alert"> Could not save: a name is required.</span>}
    </form>
  );
}
