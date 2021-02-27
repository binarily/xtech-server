package dk.comiu.server.db;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Segment {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    @ManyToOne
    Device device;
    private boolean taken = false;
}
