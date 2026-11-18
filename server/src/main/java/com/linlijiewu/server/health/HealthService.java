package com.linlijiewu.server.health;

import com.linlijiewu.server.common.ServiceUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final HealthMapper mapper;

    public HealthService(HealthMapper mapper) {
        this.mapper = mapper;
    }

    public HealthData check() {
        try {
            mapper.verifySchema();
            return new HealthData("UP", "UP", "UP");
        } catch (DataAccessException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    public record HealthData(String status, String database, String schema) {
    }
}
