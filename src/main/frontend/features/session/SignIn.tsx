import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCurrentUser } from './session';

// Sign in by typing a name; there is no password.
export function SignIn() {
  const [, setUser] = useCurrentUser();
  const [name, setName] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (trimmed) setUser(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Name <input data-testid="signin-name" value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}
