package org.example.bot.repo;

import org.example.bot.entity.Category;
import org.example.bot.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findByTitle(String text);
    List<Category> findAllByOrderByIdAsc();
}