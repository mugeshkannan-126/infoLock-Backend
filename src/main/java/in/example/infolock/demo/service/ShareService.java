package in.example.infolock.demo.service;

import in.example.infolock.demo.dto.ShareRequest;
import in.example.infolock.demo.dto.ShareResponse;
import in.example.infolock.demo.entity.Document;
import in.example.infolock.demo.entity.ShareLink;
import in.example.infolock.demo.repository.DocumentRepository;
import in.example.infolock.demo.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final DocumentRepository documentRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public ShareResponse createShareLink(ShareRequest shareRequest) {
        Document document = documentRepository.findById(shareRequest.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Optional<ShareLink> existingShare = shareLinkRepository
                .findByDocumentIdAndIsPublic(document.getId(), true);

        if (existingShare.isPresent() && Boolean.TRUE.equals(shareRequest.getIsPublic())) {
            return convertToResponse(existingShare.get());
        }

        ShareLink shareLink = ShareLink.builder()
                .token(generateUniqueToken())
                .document(document)
                .isPublic(shareRequest.getIsPublic())
                .createdDate(LocalDateTime.now())
                .expiryDate(calculateExpiryDate(shareRequest.getExpiryDays()))
                .maxViews(shareRequest.getMaxViews())
                .viewCount(0)
                .isActive(true)
                .build();

        ShareLink savedShareLink = shareLinkRepository.save(shareLink);
        return convertToResponse(savedShareLink);
    }

    public Optional<Document> getDocumentByToken(String token) {
        return shareLinkRepository.findByToken(token)
                .filter(ShareLink::getIsActive)
                .filter(shareLink -> isNotExpired(shareLink))
                .filter(shareLink -> isWithinViewLimit(shareLink))
                .map(shareLink -> {
                    shareLink.setViewCount(shareLink.getViewCount() + 1);
                    shareLinkRepository.save(shareLink);
                    return shareLink.getDocument();
                });
    }

    public boolean isDocumentOwner(Long documentId, String username) {
        System.out.println("Checking ownership for documentId: " + documentId + ", username: " + username);
        return documentRepository.findById(documentId)
                .map(document -> {
                    boolean result = document.getUser().getUsername().equals(username);
                    System.out.println("Document owner: " + document.getUser().getUsername() + ", isOwner: " + result);
                    return result;
                })
                .orElse(false);
    }

    public void toggleDocumentPublic(Long documentId, boolean isPublic) {
        Optional<ShareLink> existingShare = shareLinkRepository.findByDocumentIdAndIsPublic(documentId, true);
        if (existingShare.isPresent()) {
            ShareLink shareLink = existingShare.get();
            shareLink.setIsPublic(isPublic);
            shareLinkRepository.save(shareLink);
        } else if (isPublic) {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            ShareLink shareLink = ShareLink.builder()
                    .token(generateUniqueToken())
                    .document(document)
                    .isPublic(true)
                    .createdDate(LocalDateTime.now())
                    .expiryDate(calculateExpiryDate(30))
                    .maxViews(100)
                    .viewCount(0)
                    .isActive(true)
                    .build();
            shareLinkRepository.save(shareLink);
        }
    }

    public void deactivateShareLink(String token) {
        shareLinkRepository.findByToken(token)
                .ifPresent(shareLink -> {
                    shareLink.setIsActive(false);
                    shareLinkRepository.save(shareLink);
                });
    }

    public boolean isDocumentPublic(Long documentId) {
        return shareLinkRepository.existsByDocumentIdAndIsPublic(documentId, true);
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private LocalDateTime calculateExpiryDate(Integer expiryDays) {
        if (expiryDays == null || expiryDays <= 0) {
            return null;
        }
        return LocalDateTime.now().plusDays(expiryDays);
    }

    private boolean isNotExpired(ShareLink shareLink) {
        return shareLink.getExpiryDate() == null ||
                LocalDateTime.now().isBefore(shareLink.getExpiryDate());
    }

    private boolean isWithinViewLimit(ShareLink shareLink) {
        return shareLink.getMaxViews() == null ||
                shareLink.getViewCount() < shareLink.getMaxViews();
    }

    private ShareResponse convertToResponse(ShareLink shareLink) {
        return new ShareResponse(
                shareLink.getToken(),
                frontendUrl + "/shared/" + shareLink.getToken(),
                shareLink.getCreatedDate(),
                shareLink.getExpiryDate(),
                shareLink.getMaxViews(),
                shareLink.getIsPublic()
        );
    }
};