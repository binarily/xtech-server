package dk.comiu.server.requests;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OrderStatusResponse {
    public OrderId orderId;
    public OrderStatus status;
}
