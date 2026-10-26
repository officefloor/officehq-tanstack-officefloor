import { useState } from 'react';
import type { FormEvent } from 'react';
import { useUser } from './useUser';

export function SignIn() {
  const [, setUser] = useUser();
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (trimmed === '') {
      setError('Type a name to sign in.');
      return;
    }
    setError('');
    setUser(trimmed);
  };

  return (
    <form data-testid="signin-form" onSubmit={submit}>
      <label>
        Name{' '}
        <input
          data-testid="signin-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
      {error && <p data-testid="signin-error">{error}</p>}
    </form>
  );
}
