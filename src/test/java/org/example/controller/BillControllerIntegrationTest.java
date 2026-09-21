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

import org.springframework.http.HttpHeaders;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @WithMockUser(username = "seller", roles = {"STAFF"})
    void getPdf_ShouldReturnPdfStreamOnDemand() throws Exception {
        BillRequest.BillItemRequest item = new BillRequest.BillItemRequest();
        item.setJewelryId(jewelryRepository.findAll().get(0).getId());
        item.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setItems(Collections.singletonList(item));

        String responseStr = mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long billId = objectMapper.readTree(responseStr).get("data").get("id").asLong();

        mockMvc.perform(get("/api/bills/" + billId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bill_" + billId + ".pdf"));
    }

    @Test
    @WithMockUser(username = "seller", roles = {"STAFF"})
    void generateBill_FirstBillWhenNoPreviousBillsExist_ShouldSucceedWithCalculations() throws Exception {
        // Ensure no previous bills exist
        BillRequest.BillItemRequest item = new BillRequest.BillItemRequest();
        Jewelry jewelry = jewelryRepository.findAll().get(0);
        item.setJewelryId(jewelry.getId());
        item.setQuantity(2);

        BillRequest request = new BillRequest();
        request.setItems(Collections.singletonList(item));

        // Calculation: 5.0g * 6000.0/g * 2 = 60,000 base
        // Making: 60,000 * 10% = 6,000 -> subtotal = 66,000
        // No customer discount: discount = 0
        // GST: 66,000 * 3% = 1,980
        // Grand Total = 67,980
        mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.totalAmount").value(66000.0))
                .andExpect(jsonPath("$.data.discountAmount").value(0.0))
                .andExpect(jsonPath("$.data.gstAmount").value(1980.0))
                .andExpect(jsonPath("$.data.grandTotal").value(67980.0));
    }

    @Test
    @WithMockUser(username = "seller", roles = {"STAFF"})
    void generateBill_SubsequentBills_ShouldIncrementAndSucceed() throws Exception {
        Jewelry jewelry = jewelryRepository.findAll().get(0);

        BillRequest.BillItemRequest item1 = new BillRequest.BillItemRequest();
        item1.setJewelryId(jewelry.getId());
        item1.setQuantity(1);

        BillRequest request1 = new BillRequest();
        request1.setItems(Collections.singletonList(item1));

        // First bill
        String res1 = mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long billId1 = objectMapper.readTree(res1).get("data").get("id").asLong();

        // Second bill
        BillRequest.BillItemRequest item2 = new BillRequest.BillItemRequest();
        item2.setJewelryId(jewelry.getId());
        item2.setQuantity(1);

        BillRequest request2 = new BillRequest();
        request2.setItems(Collections.singletonList(item2));

        String res2 = mockMvc.perform(post("/api/bills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long billId2 = objectMapper.readTree(res2).get("data").get("id").asLong();

        assertTrue(billId2 > billId1, "Subsequent bill ID should be greater than previous bill ID");
    }
}
