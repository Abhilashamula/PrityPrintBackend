package com.pingprint.document;

import com.pingprint.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "storage_key", nullable = false, unique = true, length = 512) private String storageKey;
    @Column(name = "original_file_name", nullable = false, length = 255) private String originalFileName;
    @Column(name = "content_type", nullable = false, length = 128) private String contentType;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    @Column(name = "page_count", nullable = false) private int pageCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected Document() { }

    public Document(User user, String storageKey, String originalFileName, String contentType, long byteSize, int pageCount) {
        this.user = user; this.storageKey = storageKey; this.originalFileName = originalFileName;
        this.contentType = contentType; this.byteSize = byteSize; this.pageCount = pageCount; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getByteSize() { return byteSize; }
    public int getPageCount() { return pageCount; }
    public Instant getDeletedAt() { return deletedAt; }
    public void markDeleted() { deletedAt = Instant.now(); }
}
