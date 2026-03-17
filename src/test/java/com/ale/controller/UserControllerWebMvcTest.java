package com.ale.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ale.model.User;
import com.ale.repository.UserRepository;
import com.ale.service.UserSearchService;

@WebMvcTest(UserController.class)
class UserControllerWebMvcTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSearchService userSearchService;

    @Test
    void getUserByIdReturnsUser() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("James", "Smith", "james.smith", "james.smith@mail.com", TENANT_ID);
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

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
        when(userSearchService.findByFirstName(eq("James"), any(UUID.class)))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/users/search/first-name")
                        .header("X-Tenant-ID", TENANT_ID.toString())
                        .param("value", "James"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("James"));
    }
}
