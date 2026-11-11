package com.bluestaq.notes.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The caller of a request, once their credentials have been checked.
 * Controllers receive it with {@code @AuthenticationPrincipal} to learn who is calling.
 */
public record AuthenticatedUser(UUID id, String username, String passwordHash) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// There are no roles (decision 015). What a caller may do depends on ownership and team membership.
		return List.of();
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

}
