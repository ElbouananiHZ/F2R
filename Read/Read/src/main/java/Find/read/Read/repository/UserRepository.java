package Find.read.Read.repository;

// repository/UserRepository.java


import Find.read.Read.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    boolean existsByEmail(String email);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    User findByUsername(String username);

}

