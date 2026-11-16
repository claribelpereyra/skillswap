package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Colecciones en lote (@BatchSize) para evitar N+1 sin el producto cartesiano de tres fetch join.
@Entity
@Table(name = "publication")
public class PublicationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status;

    private String title;

    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    private Modality modality;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "base_price")
    private Long basePrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startsAt")
    @BatchSize(size = 50)
    private List<PublicationSlotJpaEntity> slots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "publication_tag", joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "tag")
    @BatchSize(size = 50)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "publication", cascade = CascadeType.PERSIST)
    @OrderBy("position")
    @BatchSize(size = 50)
    private List<PublicationTransitionJpaEntity> history = new ArrayList<>();

    protected PublicationJpaEntity() {
    }

    PublicationJpaEntity(String authorId, PublicationType type, Instant createdAt) {
        this.authorId = authorId;
        this.type = type;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    String getAuthorId() {
        return authorId;
    }

    PublicationType getType() {
        return type;
    }

    PublicationStatus getStatus() {
        return status;
    }

    void setStatus(PublicationStatus status) {
        this.status = status;
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    Long getCategoryId() {
        return categoryId;
    }

    void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    Modality getModality() {
        return modality;
    }

    void setModality(Modality modality) {
        this.modality = modality;
    }

    Integer getDurationMinutes() {
        return durationMinutes;
    }

    void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    int getCapacity() {
        return capacity;
    }

    void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    Long getBasePrice() {
        return basePrice;
    }

    void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    Instant getPublishedAt() {
        return publishedAt;
    }

    void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    long getVersion() {
        return version;
    }

    List<PublicationSlotJpaEntity> getSlots() {
        return slots;
    }

    Set<String> getTags() {
        return tags;
    }

    List<PublicationTransitionJpaEntity> getHistory() {
        return history;
    }
}
