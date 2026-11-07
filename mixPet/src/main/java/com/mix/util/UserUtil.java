package com.mix.util;

import com.mix.security.model.MyUser;
import org.springframework.security.core.context.SecurityContextHolder;

public interface UserUtil {
    static Object getUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        var people = auth.getPrincipal();
        if (people instanceof MyUser myUser) {
            return myUser.role();
        } else return people;
    }
}
