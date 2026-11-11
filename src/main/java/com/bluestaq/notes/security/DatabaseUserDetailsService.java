package com.bluestaq.notes.security;

import java.util.Locale;

import com.bluestaq.notes.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looks up registered users so Spring Security can check HTTP Basic credentials against them. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public DatabaseUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		// Usernames are stored in lower case (decision 035), so the lookup ignores the case typed at login.
		return users.findByUsername(username.toLowerCase(Locale.ROOT))
			.map(user -> new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash()))
			.orElseThrow(() -> new UsernameNotFoundException("No such user"));
	}

}
