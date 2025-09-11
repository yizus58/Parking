package com.nelumbo.park.service.implementations;

import com.nelumbo.park.entity.LoginLog;
import com.nelumbo.park.repository.LoginLogRepository;
import com.nelumbo.park.interfaces.ILoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements ILoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Override
    public void save(String username, String email, String description, Date date) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setEmail(email);
        loginLog.setDescription(description);
        loginLog.setDate(date);
        loginLogRepository.save(loginLog);
    }
}
