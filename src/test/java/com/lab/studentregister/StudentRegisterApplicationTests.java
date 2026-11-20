package com.lab.studentregister;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole application starts on the "test" profile: an in-memory H2 database, no MySQL needed. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentRegisterApplicationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void contextLoads() {
	}

	@Test
	void healthIsUpWithoutALogin() throws Exception {
		mvc.perform(get("/api/students/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void aStudentRecordNeedsALogin() throws Exception {
		mvc.perform(get("/api/students/1"))
				.andExpect(status().is4xxClientError());
	}
}
