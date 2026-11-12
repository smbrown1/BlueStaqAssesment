package com.bluestaq.notes.note;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ListNotesTests extends IntegrationTest {

	private String aliceId;

	@BeforeEach
	void registerUsers() throws Exception {
		aliceId = registerUser("alice");
		registerUser("bob");
	}

	private ResultActions listAs(String username, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/notes" + query).with(as(username)));
	}

	private void createNotesAsAlice(int count) throws Exception {
		for (int i = 1; i <= count; i++) {
			createNote("alice", "Note " + i, "");
		}
	}

	@Test
	void listsOnlyTheCallersOwnNotes() throws Exception {
		createNote("alice", "Alice one", "");
		createNote("alice", "Alice two", "");
		createNote("bob", "Bob one", "");

		listAs("alice", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Alice one", "Alice two")))
			.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void eachItemIsAFullNote() throws Exception {
		String noteId = createNote("alice", "Standup", "Shipped the migration.");

		listAs("alice", "")
			.andExpect(jsonPath("$.content[0].id").value(noteId))
			.andExpect(jsonPath("$.content[0].title").value("Standup"))
			.andExpect(jsonPath("$.content[0].body").value("Shipped the migration."))
			.andExpect(jsonPath("$.content[0].version").value(0))
			.andExpect(jsonPath("$.content[0].owner.id").value(aliceId))
			.andExpect(jsonPath("$.content[0].owner.username").value("alice"))
			.andExpect(jsonPath("$.content[0].updatedAt").isNotEmpty());
	}

	@Test
	void anEmptyListIsAnEmptyPage() throws Exception {
		listAs("alice", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void putsTheMostRecentlyUpdatedNoteFirst() throws Exception {
		String first = createNote("alice", "First", "");
		createNote("alice", "Second", "");
		createNote("alice", "Third", "");

		// Editing the oldest note moves it to the top.
		mockMvc.perform(put("/api/v1/notes/{id}", first).with(as("alice"))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "First, edited", "body": "", "version": 0}
				"""))
			.andExpect(status().isOk());

		listAs("alice", "")
			.andExpect(jsonPath("$.content[*].title", contains("First, edited", "Third", "Second")));
	}

	@Test
	void returns20NotesPerPageByDefault() throws Exception {
		createNotesAsAlice(21);

		listAs("alice", "")
			.andExpect(jsonPath("$.content", hasSize(20)))
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.number").value(0))
			.andExpect(jsonPath("$.page.totalElements").value(21))
			.andExpect(jsonPath("$.page.totalPages").value(2));

		listAs("alice", "?page=1")
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].title").value("Note 1"))
			.andExpect(jsonPath("$.page.number").value(1));
	}

	@Test
	void theCallerCanChooseThePageSize() throws Exception {
		createNotesAsAlice(5);

		listAs("alice", "?size=2&page=2")
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.page.size").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(3));
	}

	@Test
	void capsThePageSizeAt100() throws Exception {
		listAs("alice", "?size=500")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.size").value(100));
	}

	@Test
	void ignoresASortParameter() throws Exception {
		createNote("alice", "Older", "");
		createNote("alice", "Newer", "");

		listAs("alice", "?sort=title,asc")
			.andExpect(jsonPath("$.content[*].title", contains("Newer", "Older")));
	}

	@Test
	void aPageOrSizeThatIsNotANumberGets400() throws Exception {
		listAs("alice", "?size=lots").andExpect(status().isBadRequest());
		listAs("alice", "?page=last").andExpect(status().isBadRequest());
	}

	@Test
	void includesNotesSharedWithTheCallersTeams() throws Exception {
		String shared = createNote("alice", "Shared", "");
		createNote("alice", "Private", "");
		String teamId = createTeam("alice", "Platform");
		addMember(teamId, "bob");
		share(shared, teamId);

		listAs("bob", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].title", contains("Shared")))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void listsANoteOnceWhenItIsSharedWithTwoOfTheCallersTeams() throws Exception {
		String shared = createNote("alice", "Shared", "");
		String platform = createTeam("alice", "Platform");
		String design = createTeam("alice", "Design");
		addMember(platform, "bob");
		addMember(design, "bob");
		share(shared, platform);
		share(shared, design);

		listAs("bob", "")
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void scopeOwnedLeavesOutNotesSharedWithTheCaller() throws Exception {
		String shared = createNote("alice", "Shared", "");
		createNote("bob", "Bob's own", "");
		String teamId = createTeam("alice", "Platform");
		addMember(teamId, "bob");
		share(shared, teamId);

		listAs("bob", "?scope=owned")
			.andExpect(jsonPath("$.content[*].title", contains("Bob's own")));
	}

	@Test
	void scopeSharedLeavesOutTheCallersOwnNotes() throws Exception {
		String shared = createNote("alice", "Shared", "");
		createNote("bob", "Bob's own", "");
		String teamId = createTeam("bob", "Platform");
		share(shared, teamId);

		listAs("bob", "?scope=SHARED")
			.andExpect(jsonPath("$.content[*].title", contains("Shared")));
	}

	@Test
	void teamIdListsOnlyWhatIsSharedWithThatTeam() throws Exception {
		String shared = createNote("alice", "Shared", "");
		createNote("bob", "Bob's own", "");
		String teamId = createTeam("bob", "Platform");
		share(shared, teamId);

		listAs("bob", "?teamId=" + teamId + "&scope=owned")
			.andExpect(jsonPath("$.content[*].title", contains("Shared")));
	}

	@Test
	void teamIdForATeamTheCallerIsNotInGets403() throws Exception {
		String teamId = createTeam("alice", "Platform");

		listAs("bob", "?teamId=" + teamId)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("You are not a member of this team."));
	}

	@Test
	void teamIdForAMissingTeamGets404() throws Exception {
		listAs("alice", "?teamId=" + java.util.UUID.randomUUID())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Team not found."));
	}

	@Test
	void anUnknownScopeGets400() throws Exception {
		listAs("alice", "?scope=mine").andExpect(status().isBadRequest());
	}

	@Test
	void aTeamIdThatIsNotAUuidGets400() throws Exception {
		listAs("alice", "?teamId=nope").andExpect(status().isBadRequest());
	}

	private void addMember(String teamId, String username) throws Exception {
		mockMvc.perform(post("/api/v1/teams/{id}/members", teamId).with(as("alice"))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "%s"}
				""".formatted(username)))
			.andExpect(status().isOk());
	}

	private void share(String noteId, String teamId) throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as("alice")))
			.andExpect(status().isNoContent());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/notes"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.parameters[*].name",
				containsInAnyOrder("page", "size", "scope", "teamId")))
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].get.responses['404']").exists());
	}

}
