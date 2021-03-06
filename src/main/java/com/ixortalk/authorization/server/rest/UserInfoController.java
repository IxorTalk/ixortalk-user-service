/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.authorization.server.rest;

import com.ixortalk.authorization.server.security.thirdparty.ThirdPartyProfileService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.security.Principal;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/user")
public class UserInfoController {

    public static final String USER_INFO_CACHE_NAME = "userInfoCache";

    @Inject
    private UserProfileRestResource userProfileRestResource;

    @Inject
    private ThirdPartyProfileService thirdPartyProfileService;

    @GetMapping
    @Cacheable(cacheNames = USER_INFO_CACHE_NAME, sync = true, key = "#principal.name")
    public Object user(Principal principal) {
        if (thirdPartyOAuth2Authentication(principal)) {
            thirdPartyProfileService.refreshThirdPartyPrincipal((OAuth2Authentication) principal);
        }

        return userProfileRestResource.findByEmail(principal.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ok(principal));
    }

    @PostMapping("/evict")
    @CacheEvict(cacheNames = USER_INFO_CACHE_NAME, key = "#principal.name")
    public Object evict(Principal principal) {
        return user(principal);
    }

    private boolean thirdPartyOAuth2Authentication(Principal principal) {
        if (!(principal instanceof OAuth2Authentication)) {
            return false;
        }
        return thirdPartyAuthentication((OAuth2Authentication) principal) || refreshedAuthentication((OAuth2Authentication) principal);
    }

    private boolean refreshedAuthentication(OAuth2Authentication principal) {
        return principal.getUserAuthentication() instanceof PreAuthenticatedAuthenticationToken;
    }

    private boolean thirdPartyAuthentication(OAuth2Authentication principal) {
        return principal.getUserAuthentication() instanceof OAuth2Authentication;
    }
}

