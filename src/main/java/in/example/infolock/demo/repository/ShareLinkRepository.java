package in.example.infolock.demo.repository;

import in.example.infolock.demo.entity.Document;
import in.example.infolock.demo.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
    Optional<ShareLink> findByDocumentIdAndIsPublic(Long documentId, Boolean isPublic);
    boolean existsByDocumentIdAndIsPublic(Long documentId, Boolean isPublic);
}