package com.trello.repository;

import com.trello.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.trello.entity.User;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner(User owner);
    List<Project> findAllByOwner(User owner);

    // Public projects
    List<Project> findByType(String type);

    // Private projects owned by user
    List<Project> findByOwnerAndType(User owner, String type);

    // Private projects by ID list (keep as is, since it's a list of ids)
    List<Project> findByIdInAndType(List<Long> ids, String type);
    
    // Pagination support
    Page<Project> findAll(Pageable pageable);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId ORDER BY p.createdAt DESC")
    Page<Project> findByOwnerIdPaged(@Param("ownerId") Long ownerId, Pageable pageable);
    
    @Query("SELECT p FROM Project p WHERE p.type = :type ORDER BY p.createdAt DESC")
    Page<Project> findByTypePaged(@Param("type") String type, Pageable pageable);
}
