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

import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

@WebMvcTest(ALEUserController.class)
@Import(ALEUserControllerWebMvcTest.TestDoubles.class)
class ALEUserControllerWebMvcTest {

    private static final UUID TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ALEUserState aleUserState;

    @BeforeEach
    void resetState() {
        aleUserState.byId.clear();
        aleUserState.lastNameResults = List.of();
    }

    @Test
    void getALEUserByIdReturnsUser() throws Exception {
        UUID id = UUID.randomUUID();
        ALEUser user = new ALEUser("Mary", "Johnson", "mary.johnson", "mary.johnson@mail.com", TENANT_ID);
        user.setId(id);
        aleUserState.byId.put(id, user);

        mockMvc.perform(get("/ale-users/{id}", id)
                        .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("Mary"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
    }

    @Test
    void searchByLastNameDelegatesToService() throws Exception {
        ALEUser user = new ALEUser("Mary", "Johnson", "mary.johnson", "mary.johnson@mail.com", TENANT_ID);
        aleUserState.lastNameResults = List.of(user);

        mockMvc.perform(get("/ale-users/search/last-name")
                        .header("X-Tenant-ID", TENANT_ID.toString())
                        .param("value", "Johnson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Johnson"));
    }

    @TestConfiguration
    static class TestDoubles {

        @Bean
        ALEUserState aleUserState() {
            return new ALEUserState();
        }

        @Bean
        ALEUserRepository aleUserRepository(ALEUserState aleUserState) {
            return (ALEUserRepository) Proxy.newProxyInstance(
                    ALEUserRepository.class.getClassLoader(),
                    new Class<?>[] { ALEUserRepository.class },
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "ALEUserRepositoryTestDouble";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> method.invoke(proxy, args);
                            };
                        }
                        if ("findById".equals(method.getName())) {
                            return Optional.ofNullable(aleUserState.byId.get((UUID) args[0]));
                        }
                        throw new UnsupportedOperationException("Unsupported repository method in test: " + method.getName());
                    });
        }

        @Bean
        ALEUserSearchService aleUserSearchService(ALEUserState aleUserState) {
            return new ALEUserSearchService(null, null) {
                @Override
                public List<ALEUser> findByLastName(String lastName, UUID tenantId) {
                    return aleUserState.lastNameResults;
                }
            };
        }
    }

    static class ALEUserState {
        final HashMap<UUID, ALEUser> byId = new HashMap<>();
        List<ALEUser> lastNameResults = List.of();
    }
}
