package com.coolonlineshop.user.integration;

import com.coolonlineshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UserIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgres = new GenericContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_DB", "user_test_db")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withExposedPorts(5432);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/user_test_db".formatted(
                postgres.getHost(),
                postgres.getMappedPort(5432)
        ));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createCurrentUserStoresUserInDatabaseWithTrustedAuthData() throws Exception {
        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "attacker@example.com",
                                  "firstName": "Ivan",
                                  "lastName": "User",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("+375291112233"));

        assertTrue(userRepository.findByAuthUserId(10L).isPresent());
        assertTrue(userRepository.findByEmail("ivan.user@example.com").isPresent());
        assertTrue(userRepository.findByEmail("attacker@example.com").isEmpty());
    }

    @Test
    void createCurrentUserReturnsConflictWhenProfileAlreadyExists() throws Exception {
        createCurrentUser(10L, "ivan.user@example.com", "Ivan", "User", "+375291112233");

        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Petr",
                                  "lastName": "Petrov",
                                  "phone": "+375292223344"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User profile already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Profile for auth user 10 already exists"));
    }

    @Test
    void getCurrentUserReturnsUserFromDatabase() throws Exception {
        createCurrentUser(10L, "ivan.user@example.com", "Ivan", "User", "+375291112233");

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void updateCurrentUserChangesUserInDatabase() throws Exception {
        createCurrentUser(10L, "ivan.user@example.com", "Ivan", "User", "+375291112233");

        mockMvc.perform(put("/users/me")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Petr",
                                  "lastName": "Petrov",
                                  "phone": "+375292223344"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.phone").value("+375292223344"));

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.phone").value("+375292223344"));
    }

    @Test
    void createCurrentUserReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"));
    }

    private void createCurrentUser(
            Long authUserId,
            String email,
            String firstName,
            String lastName,
            String phone
    ) throws Exception {
        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", authUserId.toString())
                        .header("X-User-Email", email)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "phone": "%s"
                                }
                                """.formatted(firstName, lastName, phone)))
                .andExpect(status().isCreated());
    }
}
