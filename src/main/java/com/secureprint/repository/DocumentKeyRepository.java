package com.secureprint.repository;
import com.secureprint.entity.DocumentKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DocumentKeyRepository extends JpaRepository<DocumentKey, UUID> {}
