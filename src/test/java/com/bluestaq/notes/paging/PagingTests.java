package com.bluestaq.notes.paging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PagingTests {

	private static final Sort SORT = Sort.by("updatedAt");

	@Test
	void keepsAPageAndSizeThatAreInRange() {
		PageRequest request = Paging.request(3, 50, SORT);

		assertThat(request.getPageNumber()).isEqualTo(3);
		assertThat(request.getPageSize()).isEqualTo(50);
		assertThat(request.getSort()).isEqualTo(SORT);
	}

	@Test
	void capsTheSizeAt100() {
		assertThat(Paging.request(0, 100, SORT).getPageSize()).isEqualTo(100);
		assertThat(Paging.request(0, 101, SORT).getPageSize()).isEqualTo(100);
		assertThat(Paging.request(0, Integer.MAX_VALUE, SORT).getPageSize()).isEqualTo(100);
	}

	@Test
	void raisesASizeBelow1To1() {
		assertThat(Paging.request(0, 0, SORT).getPageSize()).isEqualTo(1);
		assertThat(Paging.request(0, -5, SORT).getPageSize()).isEqualTo(1);
	}

	@Test
	void treatsANegativePageAsTheFirstPage() {
		assertThat(Paging.request(-1, 20, SORT).getPageNumber()).isZero();
	}

}
