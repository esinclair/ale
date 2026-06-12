package com.ale.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.ale.model.User;
import com.ale.repository.UserRepository;
import com.ale.service.UserSearchService;

@WebMvcTest(UserController.class)
@Import(UserControllerWebMvcTest.TestDoubles.class)
class UserControllerWebMvcTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserState userState;

    @BeforeEach
    void resetState() {
        userState.byId.clear();
        userState.firstNameResults = List.of();
    }

    @Test
    void getUserByIdReturnsUser() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("James", "Smith", "james.smith", "james.smith@mail.com", TENANT_ID);
        user.setId(id);
        userState.byId.put(id, user);

        mockMvc.perform(get("/users/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("James"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
    }

    @Test
    void searchByFirstNameDelegatesToService() throws Exception {
        User user = new User("James", "Smith", "james.smith", "james.smith@mail.com", TENANT_ID);
        userState.firstNameResults = List.of(user);

        mockMvc.perform(get("/users/search/first-name")
                        .header("X-Tenant-ID", TENANT_ID.toString())
                        .param("value", "James"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("James"));
    }

    @TestConfiguration
    static class TestDoubles {

        @Bean
        UserState userState() {
            return new UserState();
        }

        @Bean
        UserRepository userRepository(UserState userState) {
            return (UserRepository) Proxy.newProxyInstance(
                    UserRepository.class.getClassLoader(),
                    new Class<?>[] { UserRepository.class },
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "UserRepositoryTestDouble";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> method.invoke(proxy, args);
                            };
                        }
                        if ("findById".equals(method.getName())) {
                            return Optional.ofNullable(userState.byId.get((UUID) args[0]));
                        }
                        throw new UnsupportedOperationException("Unsupported repository method in test: " + method.getName());
                    });
        }

        @Bean
        UserSearchService userSearchService(UserState userState) {
            return new UserSearchService(null) {
                @Override
                public List<User> findByFirstName(String firstName, UUID tenantId) {
                    return userState.firstNameResults;
                }
            };
        }
    }

    static class UserState {
        final HashMap<UUID, User> byId = new HashMap<>();
        List<User> firstNameResults = List.of();
    }
}
