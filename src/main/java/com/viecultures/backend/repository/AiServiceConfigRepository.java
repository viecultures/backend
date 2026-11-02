package com.viecultures.backend.repository;

import com.viecultures.backend.entity.AiServiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiServiceConfigRepository extends JpaRepository<AiServiceConfig, Long> {

    List<AiServiceConfig> findByProviderAndServiceTypeAndActiveTrueOrderByPriorityAsc(String provider, String serviceType);

    List<AiServiceConfig> findByProviderAndActiveTrueOrderByPriorityAsc(String provider);

    List<AiServiceConfig> findAllByOrderByProviderAscServiceTypeAscPriorityAsc();

    boolean existsByProviderAndServiceTypeAndKeyLabel(String provider, String serviceType, String keyLabel);
}
