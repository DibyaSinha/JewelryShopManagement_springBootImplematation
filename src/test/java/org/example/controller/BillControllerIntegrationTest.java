package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.BillRequest;
import org.example.entity.*;
import org.example.repository.*;
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

import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BillControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JewelryRepository jewelryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyRateRepository dailyRateRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jewelryRepository.deleteAll();
        userRepository.deleteAll();
        dailyRateRepository.deleteAll();

        // Setup prerequisites
        userRepository.save(new User(null, "seller", passwordEncoder.encode("pass"), "STAFF"));
        jewelryRepository.save(new Jewelry(null, "Gold Ring", "Co", Jewelry.MetalType.GOLD, 5.0, 10, 10.0));
        dailyRateRepository.save(new DailyRate(null, Jewelry.MetalType.GOLD, 6000.0, LocalDate.now()));
    }

    @Test
    @WithMockUser(username = "seller", roles = {"STAFF"})
    void generateBill_ShouldReturnSuccess() throws Exception {
        BillRequest.BillItemRequest item = new BillRequest.BillItemRequest();
        item.setJewelryId(jewelryRepository.findAll().get(0).getId());
        item.setQuantity(2);

        BillRequest request = new BillRequest();
        request.setItems(Collections.singletonList(item));

        mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grandTotal").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "seller", roles = {"STAFF"})
    void generateBill_WithMissingRates_ShouldReturnError() throws Exception {
        dailyRateRepository.deleteAll(); // Remove rates for today

        BillRequest.BillItemRequest item = new BillRequest.BillItemRequest();
        item.setJewelryId(jewelryRepository.findAll().get(0).getId());
        item.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setItems(Collections.singletonList(item));

        mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
