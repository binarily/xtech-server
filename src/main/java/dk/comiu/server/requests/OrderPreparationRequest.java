package dk.comiu.server.requests;

import dk.comiu.server.db.Order;

import java.util.stream.Collectors;

public class OrderPreparationRequest extends OrderRequest{
    public OrderId id;

    public static OrderPreparationRequest fromDatabase(Order order) {
        OrderPreparationRequest request = new OrderPreparationRequest();
        request.id = OrderId.fromLong(order.getId());
        request.elements = order.getParts().stream()
                .map(part -> new OrderRequest.Elements(part.getSku().getId(), part.getQuantity()))
                .collect(Collectors.toList());
        return request;
    }
}
