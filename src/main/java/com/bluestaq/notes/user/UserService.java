package com.bluestaq.notes.user;

import java.util.Locale;

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

	/** Registers a user. The username is stored in lower case and the password as a hash. */
	@Transactional
	public User register(String username, String password) {
		String normalized = username.toLowerCase(Locale.ROOT);
		return users.save(new User(normalized, passwordEncoder.encode(password)));
	}

}
