package com.assignment.datasetapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "dataset_records",
    // Index on datasetName for faster GROUP BY / SORT BY queries
    indexes = {
        @Index(name = "idx_dataset_name", columnList = "dataset_name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetRecord {

    /**
     * Primary Key — auto-incremented by the database.
     * This is the internal DB ID, different from the user-supplied "id" in JSON.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_name", nullable = false, length = 255)
    private String datasetName;

    @Lob
    @Column(name = "record_data", nullable = false, columnDefinition = "TEXT")
    private String recordData;

    /**
     * Audit field: automatically set when record is created.
     * @CreationTimestamp is a Hibernate annotation that fills this automatically.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
