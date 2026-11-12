package com.bluestaq.notes.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** The paging rules shared by every list endpoint (decision 044). */
public final class Paging {

	/** Page size used when the caller does not ask for one. Kept as text for use in annotations. */
	public static final String DEFAULT_SIZE = "20";

	/** The largest page a caller may ask for. */
	public static final int MAX_SIZE = 100;

	private Paging() {
	}

	/**
	 * Builds a page request with a fixed sort order. Values out of range are brought into range
	 * rather than rejected: the size is kept between 1 and {@link #MAX_SIZE}, and a negative
	 * page number means the first page.
	 */
	public static PageRequest request(int page, int size, Sort sort) {
		return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE), sort);
	}

}
