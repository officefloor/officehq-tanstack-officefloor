import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCurrentUser } from './session';

// Sign in by typing a name; there is no password.
export function SignIn() {
  const [user, setUser] = useCurrentUser();
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  if (user) {
    return (
      <span>
        Signed in as <strong data-testid="current-user">{user}</strong>{' '}
        <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
          Sign out
        </button>
      </span>
    );
  }

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Type a name to sign in.');
      return;
    }
    setError('');
    setUser(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="signin-name"
        placeholder="Your name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
      {error && <span data-testid="signin-error">{error}</span>}
    </form>
  );
}
