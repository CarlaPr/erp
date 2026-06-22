package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {}