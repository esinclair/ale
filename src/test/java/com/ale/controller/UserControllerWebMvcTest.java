package com.ale.controller;

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

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSearchService userSearchService;

    @Test
    void getUserByIdReturnsUser() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("James", "Smith", "james.smith", "james.smith@mail.com");
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("James"));
    }

    @Test
    void searchByFirstNameDelegatesToService() throws Exception {
        User user = new User("James", "Smith", "james.smith", "james.smith@mail.com");
        when(userSearchService.findByFirstName("James")).thenReturn(List.of(user));

        mockMvc.perform(get("/users/search/first-name").param("value", "James"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("James"));
    }
}
