package dk.comiu.server.requests;

import dk.comiu.server.db.Colour;
import dk.comiu.server.db.Order;
import lombok.AllArgsConstructor;

public class OrderColourResponse extends OrderStatusResponse {
    public OrderColour colour;

    public OrderColourResponse(OrderId orderId, OrderStatus status, OrderColour colour) {
        super(orderId, status);
        this.colour = colour;
    }

    public static OrderColourResponse fromDatabase(Order order) {
        return new OrderColourResponse(OrderId.fromLong(order.getId()),
                OrderStatus.OK,
                OrderColour.fromDatabase(order.getColour()));
    }

    public OrderColourResponse(OrderId orderId, OrderStatus status) {
        super(orderId, status);
    }

    @AllArgsConstructor
    public static class OrderColour {
        public long colour;
        public long segment;

        public static OrderColour fromDatabase(Colour colour) {
            return new OrderColour(colour.getId(), colour.getSegment().getId());
        }
    }
}
