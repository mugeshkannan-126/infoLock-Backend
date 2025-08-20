package in.example.infolock.demo.controllers;

import in.example.infolock.demo.dto.ShareRequest;
import in.example.infolock.demo.dto.ShareResponse;
import in.example.infolock.demo.entity.Document;
import in.example.infolock.demo.service.ShareService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/share")
    public ResponseEntity<?> createShareLink(
            @RequestBody ShareRequest shareRequest,
            Authentication authentication) {
        String username = authentication.getName();
        if (!shareService.isDocumentOwner(shareRequest.getDocumentId(), username)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "FORBIDDEN");
            errorResponse.put("message", "You do not have permission to share this document");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        try {
            ShareResponse response = shareService.createShareLink(shareRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage().contains("Document not found") ? "NOT_FOUND" : "SHARE_ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(e.getMessage().contains("Document not found") ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/share/{token}")
    public void viewSharedDocument(
            @PathVariable String token,
            HttpServletResponse response) throws IOException {
        Optional<Document> document = shareService.getDocumentByToken(token);

        if (document.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Shared document not found or expired");
            return;
        }

        Document doc = document.get();
        response.setContentType(getContentType(doc.getFileType()));
        response.setHeader("Content-Disposition", "inline; filename=\"" + doc.getFileName() + "\"");
        response.setContentLengthLong(doc.getFileData().length);
        response.getOutputStream().write(doc.getFileData());
        response.getOutputStream().flush();
    }

    @DeleteMapping("/share/{token}")
    public ResponseEntity<Void> deactivateShareLink(
            @PathVariable String token,
            Authentication authentication) {
        shareService.deactivateShareLink(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{documentId}/is-public")
    public ResponseEntity<Boolean> isDocumentPublic(@PathVariable Long documentId) {
        boolean isPublic = shareService.isDocumentPublic(documentId);
        return ResponseEntity.ok(isPublic);
    }

    @PutMapping("/{documentId}/toggle-public")
    public ResponseEntity<?> toggleDocumentPublic(
            @PathVariable Long documentId,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {
        String username = authentication.getName();
        if (!shareService.isDocumentOwner(documentId, username)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "FORBIDDEN");
            errorResponse.put("message", "You do not have permission to modify this document");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        try {
            boolean isPublic = request.getOrDefault("isPublic", false);
            shareService.toggleDocumentPublic(documentId, isPublic);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "UPDATE_ERROR");
            errorResponse.put("message", "Failed to update public status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String getContentType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}