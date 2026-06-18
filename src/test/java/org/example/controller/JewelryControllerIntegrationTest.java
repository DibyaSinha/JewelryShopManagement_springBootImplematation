package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Jewelry;
import org.example.entity.User;
import org.example.repository.JewelryRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class JewelryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JewelryRepository jewelryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jewelryRepository.deleteAll();
        userRepository.deleteAll();

        // Create an admin user for security tests if needed
        User admin = new User(null, "admin", passwordEncoder.encode("admin"), "ADMIN");
        userRepository.save(admin);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addJewelry_AsAdmin_ShouldReturnCreated() throws Exception {
        Jewelry jewelry = new Jewelry(null, "Silver Necklace", "Chandi", Jewelry.MetalType.SILVER, 20.0, 5, 12.0);

        mockMvc.perform(post("/api/jewelry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jewelry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Silver Necklace"));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void addJewelry_AsStaff_ShouldReturnForbidden() throws Exception {
        Jewelry jewelry = new Jewelry(null, "Restricted Item", "Co", Jewelry.MetalType.GOLD, 10.0, 1, 10.0);

        mockMvc.perform(post("/api/jewelry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(jewelry)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user")
    void getAllJewelry_ShouldReturnList() throws Exception {
        jewelryRepository.save(new Jewelry(null, "Item 1", "Co", Jewelry.MetalType.GOLD, 1.0, 10, 5.0));
        jewelryRepository.save(new Jewelry(null, "Item 2", "Co", Jewelry.MetalType.SILVER, 2.0, 20, 5.0));

        mockMvc.perform(get("/api/jewelry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteJewelry_AsAdmin_ShouldReturnSuccess() throws Exception {
        Jewelry saved = jewelryRepository.save(new Jewelry(null, "Delete Me", "Co", Jewelry.MetalType.GOLD, 1.0, 1, 1.0));

        mockMvc.perform(delete("/api/jewelry/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Jewelry deleted"));
        
        assertFalse(jewelryRepository.existsById(saved.getId()));
    }

    private void assertFalse(boolean condition) {
        if (condition) throw new AssertionError("Expected false but was true");
    }
}
