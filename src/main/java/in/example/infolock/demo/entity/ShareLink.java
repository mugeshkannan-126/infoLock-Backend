package in.example.infolock.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @Column
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private Integer maxViews;

    @Column
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Boolean isPublic = false;
}