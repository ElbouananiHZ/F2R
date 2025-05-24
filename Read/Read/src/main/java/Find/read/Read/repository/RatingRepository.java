package Find.read.Read.repository;

import Find.read.Read.models.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends MongoRepository<Rating, String> {
    Optional<Rating> findByNovelIdAndUserId(String novelId, String userId);
    List<Rating> findByNovelId(String novelId);
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.novelId = :novelId")
    Double findAverageRatingByNovelId(@Param("novelId") String novelId);
}
