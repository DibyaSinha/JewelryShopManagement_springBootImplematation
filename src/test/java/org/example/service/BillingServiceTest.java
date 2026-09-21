package org.example.service;

import org.example.dto.BillRequest;
import org.example.entity.*;
import org.example.exception.InsufficientStockException;
import org.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BillingServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private JewelryRepository jewelryRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private DailyRateRepository dailyRateRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvoiceService invoiceService;

    @InjectMocks
    private BillingService billingService;

    private User seller;
    private Jewelry goldJewelry;
    private DailyRate goldRate;
    private Customer customer;

    @BeforeEach
    void setUp() {
        seller = new User(1L, "seller", "pass", "STAFF");
        goldJewelry = new Jewelry(1L, "Ring", "Co", Jewelry.MetalType.GOLD, 10.0, 5, 10.0);
        goldRate = new DailyRate(1L, Jewelry.MetalType.GOLD, 5000.0, LocalDate.now());
        customer = new Customer("1234567890", "John", 5.0);
    }

    @Test
    void generateBill_ShouldCalculateCorrectly() {
        // Arrange
        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setCustomerMobile("1234567890");
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(customerRepository.findById("1234567890")).thenReturn(Optional.of(customer));
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(goldJewelry));
        when(dailyRateRepository.findByMetalTypeAndRateDate(any(), any())).thenReturn(Optional.of(goldRate));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Bill result = billingService.generateBill(request, 1L);

        // Assert
        // ...
        assertNotNull(result);
        assertEquals(55000.0, result.getTotalAmount());
        assertEquals(2750.0, result.getDiscountAmount());
        assertEquals(1567.5, result.getGstAmount());
        assertEquals(53817.5, result.getGrandTotal());
        verify(jewelryRepository, times(1)).save(any());
    }

    @Test
    void generateBill_WhenJewelryVersionIsNull_ShouldSafelyInitializeVersionAndSave() {
        // Arrange jewelry with null version
        goldJewelry.setVersion(null);

        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setCustomerMobile("1234567890");
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(customerRepository.findById("1234567890")).thenReturn(Optional.of(customer));
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(goldJewelry));
        when(dailyRateRepository.findByMetalTypeAndRateDate(any(), any())).thenReturn(Optional.of(goldRate));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Bill result = billingService.generateBill(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(0L, goldJewelry.getVersion());
        assertEquals(4, goldJewelry.getStock());
        verify(jewelryRepository, times(1)).save(goldJewelry);
    }

    @Test
    void generateBill_WhenInsufficientStock_ShouldThrowException() {
        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(10); // More than stock (5)

        BillRequest request = new BillRequest();
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(goldJewelry));

        assertThrows(InsufficientStockException.class, () -> billingService.generateBill(request, 1L));
    }

    @Test
    void getBillPdf_WhenBillExists_ShouldReturnGeneratedPdf() {
        Bill bill = new Bill();
        bill.setId(10L);
        byte[] expectedPdf = new byte[]{1, 2, 3};

        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));
        when(invoiceService.generatePdf(bill)).thenReturn(expectedPdf);

        byte[] result = billingService.getBillPdf(10L);

        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        verify(invoiceService, times(1)).generatePdf(bill);
    }

    @Test
    void getBillPdf_WhenBillNotFound_ShouldReturnNull() {
        when(billRepository.findById(99L)).thenReturn(Optional.empty());

        byte[] result = billingService.getBillPdf(99L);

        assertNull(result);
        verify(invoiceService, never()).generatePdf(any());
    }

    @Test
    void generateBill_WhenNewCustomerWithValidName_ShouldCreateCustomerAndLinkToBill() {
        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setCustomerMobile("9998887776");
        request.setCustomerName("Rahul Kumar");
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(customerRepository.findById("9998887776")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(goldJewelry));
        when(dailyRateRepository.findByMetalTypeAndRateDate(any(), any())).thenReturn(Optional.of(goldRate));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        Bill result = billingService.generateBill(request, 1L);

        assertNotNull(result);
        assertNotNull(result.getCustomer());
        assertEquals("9998887776", result.getCustomer().getMobileNumber());
        assertEquals("Rahul Kumar", result.getCustomer().getName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void generateBill_WhenNewCustomerWithoutName_ShouldThrowException() {
        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setCustomerMobile("9998887776");
        request.setCustomerName(null);
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(customerRepository.findById("9998887776")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> billingService.generateBill(request, 1L));
        assertTrue(exception.getMessage().contains("Customer name is required"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void generateBill_WhenNoCustomerMobile_ShouldCreateBillWithoutCustomer() {
        BillRequest.BillItemRequest itemReq = new BillRequest.BillItemRequest();
        itemReq.setJewelryId(1L);
        itemReq.setQuantity(1);

        BillRequest request = new BillRequest();
        request.setCustomerMobile(null);
        request.setItems(Collections.singletonList(itemReq));

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(goldJewelry));
        when(dailyRateRepository.findByMetalTypeAndRateDate(any(), any())).thenReturn(Optional.of(goldRate));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        Bill result = billingService.generateBill(request, 1L);

        assertNotNull(result);
        assertNull(result.getCustomer());
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).save(any());
    }
}
