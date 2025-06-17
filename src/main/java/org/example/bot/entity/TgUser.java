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
public class TgUser {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private State state;

    private String username;


    private String tempCategoryTitle;
}
