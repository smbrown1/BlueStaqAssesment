package com.bluestaq.notes.user;

import java.util.Locale;
import java.util.UUID;

import com.bluestaq.notes.error.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Registers a user. The username is stored in lower case and the password as a hash.
	 *
	 * @throws ConflictException if the username is already taken
	 */
	@Transactional
	public User register(String username, String password) {
		String normalized = username.toLowerCase(Locale.ROOT);
		if (users.existsByUsername(normalized)) {
			throw usernameTaken(normalized);
		}
		try {
			return users.saveAndFlush(new User(normalized, passwordEncoder.encode(password)));
		}
		catch (DataIntegrityViolationException ex) {
			// Two registrations for the same name arrived together and the other one won.
			// The unique constraint on the column is what finally decides.
			throw usernameTaken(normalized);
		}
	}

	/** Returns the user with the given id, which must exist. */
	@Transactional(readOnly = true)
	public User get(UUID id) {
		return users.findById(id).orElseThrow();
	}

	private static ConflictException usernameTaken(String username) {
		return new ConflictException("The username '" + username + "' is already taken.");
	}

}
