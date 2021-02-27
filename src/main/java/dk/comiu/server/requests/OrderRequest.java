package dk.comiu.server.requests;

import lombok.AllArgsConstructor;

import java.util.List;

public class OrderRequest {
    public List<Elements> elements;

    @AllArgsConstructor
    public static class Elements {
        public long sku;
        public long quantity;
    }
}
