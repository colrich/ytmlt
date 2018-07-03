package io.gridbug.ytu.ytutility.dao;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import io.gridbug.ytu.ytutility.model.Subscription;

public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    Optional<Subscription> findByYtId(String ytId);

    @Query("select s from Subscription s")
    Stream<Subscription> findSubscriptions();

} 
