import { useState } from 'react';
import { useUser } from './session';

/** Sign in by typing a name; there is no password. */
export function SignIn() {
  const [, setUser] = useUser();
  const [name, setName] = useState('');
  const [error, setError] = useState(false);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
          setError(true);
          return;
        }
        setUser(trimmed);
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="signin-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError(false);
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
      {error && <span data-testid="signin-error">Type a name to sign in.</span>}
    </form>
  );
}
