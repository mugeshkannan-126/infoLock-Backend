package in.example.infolock.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {
    private String shareToken;
    private String shareUrl;
    private LocalDateTime createdDate;
    private LocalDateTime expiryDate;
    private Integer maxViews;
    private Boolean isPublic;
}