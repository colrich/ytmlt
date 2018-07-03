package io.gridbug.ytu.ytutility.dao;

import org.springframework.data.repository.CrudRepository;

import io.gridbug.ytu.ytutility.model.TestItem;

public interface TestItemRepository extends CrudRepository<TestItem, Long> {}
