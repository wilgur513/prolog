package wooteco.prolog.tag.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostTagDao {

    private final JdbcTemplate jdbcTemplate;

    public PostTagDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long postId, Long tagId) {
        String query = "INSERT INTO postTag(post_id, tag_id) VALUES(?, ?)";
        this.jdbcTemplate.update(query, postId, tagId);
    }

    public List<Long> findByPostId(Long postId) {
        String query = "SELECT tag_id FROM postTag WHERE post_id = ?";
        return jdbcTemplate.queryForList(query, Long.class, postId);
    }
}