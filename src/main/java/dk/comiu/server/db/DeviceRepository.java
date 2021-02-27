package dk.comiu.server.db;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface DeviceRepository extends CrudRepository<Device, Long> {

    Optional<Device> findByName(String name);
    Device findFirstByType(Device.DeviceType type);
    Device findFirstByTypeAndId(Device.DeviceType type, long id);
}