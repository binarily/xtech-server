package dk.comiu.server.requests;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class OrderId {
    public long id;

    public static OrderId fromLong(long id) {
        return new OrderId(id);
    }
}
