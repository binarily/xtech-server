package dk.comiu.server.db;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Colour {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    @ManyToOne
    private Device device;
    private String name;
    @OneToOne(optional = true)
    private Segment segment;
}
