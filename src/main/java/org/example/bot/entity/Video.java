package org.example.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Video {
    @Id
    private Integer id;
    private String title;
    private Integer fileId;
    @ManyToOne
    private Category category;
}
