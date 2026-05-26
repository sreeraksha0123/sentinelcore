package com.raksha.sentinelcore.repository;

import com.raksha.sentinelcore.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId")
    Optional<Subscription> findByUserId(@Param("userId") Long userId);
}
