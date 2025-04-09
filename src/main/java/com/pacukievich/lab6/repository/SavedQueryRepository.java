package com.pacukievich.lab6.repository;

import com.pacukievich.lab6.model.SavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {

}