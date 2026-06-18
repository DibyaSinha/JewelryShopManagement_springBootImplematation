package org.example.service;

import org.example.entity.Jewelry;
import org.example.repository.JewelryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JewelryServiceTest {

    @Mock
    private JewelryRepository jewelryRepository;

    @InjectMocks
    private JewelryService jewelryService;

    private Jewelry sampleJewelry;

    @BeforeEach
    void setUp() {
        sampleJewelry = new Jewelry(1L, "Gold Ring", "Chandi Fashion", Jewelry.MetalType.GOLD, 5.0, 10, 15.0);
    }

    @Test
    void getAllJewelry_ShouldReturnList() {
        when(jewelryRepository.findAll()).thenReturn(Arrays.asList(sampleJewelry));
        List<Jewelry> result = jewelryService.getAllJewelry();
        assertEquals(1, result.size());
        assertEquals("Gold Ring", result.get(0).getName());
    }

    @Test
    void getJewelryById_WhenExists_ShouldReturnItem() {
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(sampleJewelry));
        Jewelry result = jewelryService.getJewelryById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getJewelryById_WhenNotExists_ShouldReturnNull() {
        when(jewelryRepository.findById(99L)).thenReturn(Optional.empty());
        Jewelry result = jewelryService.getJewelryById(99L);
        assertNull(result);
    }

    @Test
    void addJewelry_ShouldSaveAndReturn() {
        when(jewelryRepository.save(any(Jewelry.class))).thenReturn(sampleJewelry);
        Jewelry result = jewelryService.addJewelry(new Jewelry());
        assertNotNull(result);
        assertEquals("Gold Ring", result.getName());
    }

    @Test
    void updateJewelry_WhenExists_ShouldUpdateAndReturn() {
        Jewelry updatedDetails = new Jewelry(null, "Updated Ring", "New Co", Jewelry.MetalType.GOLD, 6.0, 20, 10.0);
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(sampleJewelry));
        when(jewelryRepository.save(any(Jewelry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jewelry result = jewelryService.updateJewelry(1L, updatedDetails);

        assertNotNull(result);
        assertEquals("Updated Ring", result.getName());
        assertEquals(20, result.getStock());
    }

    @Test
    void addStock_ShouldIncrementAndSave() {
        when(jewelryRepository.findById(1L)).thenReturn(Optional.of(sampleJewelry));
        when(jewelryRepository.save(any(Jewelry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jewelry result = jewelryService.addStock(1L, 5);

        assertNotNull(result);
        assertEquals(15, result.getStock()); // 10 + 5
    }

    @Test
    void deleteJewelry_ShouldCallRepository() {
        doNothing().when(jewelryRepository).deleteById(1L);
        jewelryService.deleteJewelry(1L);
        verify(jewelryRepository, times(1)).deleteById(1L);
    }
}
