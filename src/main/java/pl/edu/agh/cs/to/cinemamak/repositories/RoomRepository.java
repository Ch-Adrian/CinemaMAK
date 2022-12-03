package pl.edu.agh.cs.to.cinemamak.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.cs.to.cinemamak.models.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
}
