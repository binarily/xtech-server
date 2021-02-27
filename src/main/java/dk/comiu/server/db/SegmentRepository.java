package dk.comiu.server.db;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.Optional;

public interface SegmentRepository extends CrudRepository<Segment, Long> {
    Optional<Segment> findFirstByDeviceAndTakenIsFalse(Device device);

    @Transactional
    @Modifying
    default Optional<Segment> getFirstAvailableSegment(Device device) {
        return findFirstByDeviceAndTakenIsFalse(device)
                .map(segment -> {
                    segment.setTaken(true);
                    return save(segment);
                });
    }
}