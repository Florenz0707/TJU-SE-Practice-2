package cn.edu.tju.product.service;

import cn.edu.tju.product.service.dto.BusinessDto;
import cn.edu.tju.product.util.AuthorityUtils;
import cn.edu.tju.product.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MerchantBusinessService {

    private final MerchantClient merchantClient;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    public MerchantBusinessService(MerchantClient merchantClient, JwtUtils jwtUtils, HttpServletRequest request) {
        this.merchantClient = merchantClient;
        this.jwtUtils = jwtUtils;
        this.request = request;
    }

    /**
     * 解析当前请求身份。
     * - 未登录：Optional.empty()
     * - 已登录：返回 userId + authorities
     */
    public Optional<Actor> getActor() {
        String raw = request.getHeader("Authorization");
        String token = raw;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long uid = jwtUtils.getUserId(token);
        if (uid == null) return Optional.empty();
        Set<String> auth = AuthorityUtils.getAuthoritySet(jwtUtils.getAuthorities(token));
        return Optional.of(new Actor(uid, auth));
    }

    public Optional<Long> getBusinessOwnerId(Long businessId) {
        Optional<BusinessDto> business = merchantClient.getBusinessById(businessId);
        return business.map(BusinessDto::getBusinessOwnerId);
    }

    /**
     * 返回“当前登录商家”的店铺 id（若无店铺则 empty）。
     * 依赖 merchant-service 的 /api/businesses/my。
     */
    public Optional<Long> getMyBusinessId() {
        Optional<Actor> actorOpt = getActor();
        if (actorOpt.isEmpty()) return Optional.empty();
        Actor actor = actorOpt.get();
        if (!actor.isBusiness() && !actor.isAdmin()) return Optional.empty();

        try {
            List<BusinessDto> my = merchantClient.getMyBusinesses();
            if (my == null || my.isEmpty()) return Optional.empty();
            // 约定：每个商家一个活跃店铺
            Long id = my.get(0).getId();
            return Optional.ofNullable(id);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    /**
     * 对“菜单管理”用途：商家只允许查询自己名下店铺。
     * 当前实现：只做单店铺查询（businessId 必填），因此这里返回 empty。
     * 之后如果 merchant-service 增加“查我的店铺列表”的接口，可在这里扩展。
     */
    public List<Long> getMyBusinessIds() {
        return Collections.emptyList();
    }

    public record Actor(Long userId, Set<String> authorities) {
        public boolean isAdmin() {
            return AuthorityUtils.hasAuthority(authorities, "ADMIN");
        }

        public boolean isBusiness() {
            return AuthorityUtils.hasAuthority(authorities, "BUSINESS");
        }
    }
}
