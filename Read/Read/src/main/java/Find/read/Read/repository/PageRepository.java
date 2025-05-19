package Find.read.Read.repository;

import Find.read.Read.models.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends MongoRepository<Page, String> {
    Optional<Page> findByNovelIdAndPageNumber(String novelId, int pageNumber);
    // Add method to find all pages by novelId
    List<Page> findByNovelId(String novelId);
    // Add any other necessary custom queries if needed


}
