package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.LoginLog;

public interface LoginLogRepositoryPort {

    void save(LoginLog loginLog);
}
