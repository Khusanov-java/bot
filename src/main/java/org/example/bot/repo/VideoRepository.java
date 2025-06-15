package org.example.bot.repo;

import org.example.bot.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Integer> {
    List<Video> findByCategory_Id(Integer id);

    Video findByTitle(String text);
}