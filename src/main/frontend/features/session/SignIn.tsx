import { useState } from 'react';
import type { FormEvent } from 'react';
import { useUser } from './session';

/** Sign in by typing a name. */
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
      {error && <p data-testid="signin-error" role="alert">{error}</p>}
    </form>
  );
}

/** Who is signed in, and a way to sign out. */
export function CurrentUser({ user }: { user: string }) {
  const [, setUser] = useUser();
  return (
    <p>
      Signed in as <strong data-testid="current-user">{user}</strong>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
        Sign out
      </button>
    </p>
  );
}
