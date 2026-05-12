package com.lostfound.backend.service;

import com.lostfound.backend.dto.LoginDTO;
import com.lostfound.backend.dto.RegisterDTO;
import com.lostfound.backend.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
}