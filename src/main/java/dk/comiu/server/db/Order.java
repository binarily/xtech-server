package dk.comiu.server.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "orders")
@With
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderPart> parts;
    private BigDecimal totalAmount;
    private OrderState state;
    private LocalDateTime created;
    private LocalDateTime updated;
    @ManyToOne
    private Device createdBy;
    @ManyToOne
    private Device updatedBy;
    @ManyToOne
    private Colour colour;

    public enum OrderState {
        //When we receive it from table
        CREATED,
        //When directed to payment processor (e.g., MobilePay)
        SENT_TO_PAYMENT_PROCESSOR,
        //When payment processor returns an error
        PAYMENT_PROCESSOR_ERROR,
        //When payment processor accepts payment
        ACCEPTED_BY_PAYMENT_PROCESSOR,
        //When payment processor rejects payment
        REJECTED_BY_PAYMENT_PROCESSOR,
        //When order sent to counter device
        PREPARING,
        //When order is marked by counter device as ready
        READY,
        //When ready state is not relayed correctly
        READY_ERROR,
        //When order is marked by counter device as done
        DONE,
        //When done state is not relayed correctly
        DONE_ERROR,
        //When user cancels the order (only before payment is accepted)
        CANCELLED
    }

    @Entity
    @NoArgsConstructor
    @Data
    public static class OrderPart {
        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        @ManyToOne
        private Sku sku;
        private long quantity;

        public OrderPart(Sku sku, long quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }
    }

    public static Order fromRequest(List<OrderPart> orderParts, Device device) {
        BigDecimal totalAmount = orderParts.stream()
                .map(part -> part.getSku().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Order()
                .withParts(orderParts)
                .withTotalAmount(totalAmount)
                .withState(OrderState.CREATED)
                .withCreated(LocalDateTime.now())
                .withUpdated(LocalDateTime.now())
                .withCreatedBy(device)
                .withUpdatedBy(device);
    }
}