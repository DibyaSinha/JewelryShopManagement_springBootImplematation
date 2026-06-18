package org.example.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BillRequest {
    private String customerMobile;
    
    @NotEmpty(message = "Bill must have at least one item")
    private List<BillItemRequest> items;

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }
    public List<BillItemRequest> getItems() { return items; }
    public void setItems(List<BillItemRequest> items) { this.items = items; }

    public static class BillItemRequest {
        private Long jewelryId;
        private Integer quantity;

        public Long getJewelryId() { return jewelryId; }
        public void setJewelryId(Long jewelryId) { this.jewelryId = jewelryId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
