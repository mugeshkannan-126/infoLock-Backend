package in.example.infolock.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequest {
    private Long documentId;
    private Boolean isPublic;
    private Integer expiryDays; // Optional: expiry in days
    private Integer maxViews;   // Optional: maximum views
}