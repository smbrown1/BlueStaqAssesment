package com.bluestaq.notes.paging;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Makes every {@code Page} a controller returns serialize in Spring Data's stable format:
 * {@code {"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}} (decision 045).
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class PagingConfig {
}
