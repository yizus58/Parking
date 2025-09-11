package com.nelumbo.park.interfaces;

import java.util.Date;

public interface ILoginLogService {
    void save(String username, String email, String description, Date date);
}
