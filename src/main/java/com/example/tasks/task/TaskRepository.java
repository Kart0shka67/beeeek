package com.example.tasks.task;

import com.example.tasks.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwner_Id(long userId);

    @Query("SELECT t FROM Task t WHERE t.deleted = false AND t.owner.id = :userId")
    List<Task> findActiveByOwnerId(@Param("userId") long userId);

    @Query("SELECT t FROM Task t WHERE t.deleted = false AND :userId IN (SELECT a.id FROM t.assignees a)")
    List<Task> findActiveAssignedTo(@Param("userId") long userId);

    // Admin: all tasks including deleted
    List<Task> findByOwner(User owner);

    @Query("SELECT t FROM Task t WHERE t.deleted = true")
    List<Task> findAllDeleted();

    @Query("SELECT t FROM Task t WHERE t.deleted = false")
    List<Task> findAllActive();
}
