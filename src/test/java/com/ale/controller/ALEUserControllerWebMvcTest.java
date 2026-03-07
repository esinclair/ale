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

import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;
import com.ale.service.ALEUserSearchService;

@WebMvcTest(ALEUserController.class)
class ALEUserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ALEUserRepository aleUserRepository;

    @MockBean
    private ALEUserSearchService aleUserSearchService;

    @Test
    void getALEUserByIdReturnsUser() throws Exception {
        UUID id = UUID.randomUUID();
        ALEUser user = new ALEUser("Mary", "Johnson", "mary.johnson", "mary.johnson@mail.com");
        user.setId(id);

        when(aleUserRepository.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/ale-users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("Mary"));
    }

    @Test
    void searchByLastNameDelegatesToService() throws Exception {
        ALEUser user = new ALEUser("Mary", "Johnson", "mary.johnson", "mary.johnson@mail.com");
        when(aleUserSearchService.findByLastName("Johnson")).thenReturn(List.of(user));

        mockMvc.perform(get("/ale-users/search/last-name").param("value", "Johnson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Johnson"));
    }
}
