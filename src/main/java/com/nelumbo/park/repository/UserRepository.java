package com.nelumbo.park.repository;

import com.nelumbo.park.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    User findByEmailWithRole(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.username = :name")
    User findByName(String name);
    
    @Query("SELECT u FROM User u WHERE u.id = :id")
    User findByIdUser(@Param("id") String id);
}
