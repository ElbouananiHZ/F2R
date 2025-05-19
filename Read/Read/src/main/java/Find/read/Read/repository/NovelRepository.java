package Find.read.Read.repository;

import Find.read.Read.controller.NovelController;
import Find.read.Read.enums.NovelCategory;
import Find.read.Read.enums.NovelTag;
import Find.read.Read.models.Novel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelRepository extends MongoRepository<Novel, String> {
    Optional<Novel> findById(String id);



    List<Novel> findByAuthorId(String authorId);
    List<Novel> findByNameContainingIgnoreCase(String name);
    @Query("{ 'tags': { $in: ?0 } }")
    List<Novel> findByTagsIn(List<NovelTag> tags);
    @Query("{ 'tags': { $in: ?0 } }")
    List<Novel> findByTagsIn(List<NovelTag> tags, Sort sort);
    @Query(value = "{}", sort = "{ 'averageRating' : -1 }")
    List<Novel> findAllByRatingDesc();




}
