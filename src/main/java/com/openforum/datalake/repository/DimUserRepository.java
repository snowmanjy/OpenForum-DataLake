package com.openforum.datalake.repository;

import com.openforum.datalake.domain.DimUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DimUserRepository extends JpaRepository<DimUser, UUID> {
}
