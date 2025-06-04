package com.smartcampus.examgrading.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "material_files")
@Data
@NoArgsConstructor
public class MaterialFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;
}