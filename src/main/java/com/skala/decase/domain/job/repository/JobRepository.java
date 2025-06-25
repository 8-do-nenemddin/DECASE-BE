package com.skala.decase.domain.job.repository;

import com.skala.decase.domain.job.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
}
