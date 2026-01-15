package com.second.hand.trading.server.service.impl;

import com.second.hand.trading.server.dao.AddressDao;
import com.second.hand.trading.server.dao.IdleItemDao;
import com.second.hand.trading.server.dao.UserDao;
import com.second.hand.trading.server.model.AddressModel;
import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.model.UserModel;
import com.second.hand.trading.server.service.RecommendationService;
import com.second.hand.trading.server.vo.HomeRecommendVo;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_HOT_LIMIT = 8;
    private static final int DEFAULT_LATEST_LIMIT = 8;
    private static final int DEFAULT_LOCAL_LIMIT = 8;
    private static final int DEFAULT_CONTENT_LIMIT = 12;
    private static final Pattern TOKEN_SANITIZER = Pattern.compile("[^A-Za-z0-9\\u4e00-\\u9fa5\\s]");

    @Resource
    private IdleItemDao idleItemDao;

    @Resource
    private AddressDao addressDao;

    @Resource
    private UserDao userDao;

    @Override
    public HomeRecommendVo getHomeRecommendations(Long userId, Integer hotLimit,
                                                   Integer latestLimit, Integer localLimit) {
        int hotSize = normalizeLimit(hotLimit, DEFAULT_HOT_LIMIT);
        int latestSize = normalizeLimit(latestLimit, DEFAULT_LATEST_LIMIT);
        int localSize = normalizeLimit(localLimit, DEFAULT_LOCAL_LIMIT);

        List<IdleItemModel> hotItems = idleItemDao.listHotItems(hotSize);
        List<IdleItemModel> latestItems = idleItemDao.listLatestItems(latestSize);

        List<IdleItemModel> localItems = Collections.emptyList();
        String city = resolveCity(userId);
        if (city != null && !city.isEmpty()) {
            localItems = idleItemDao.listByCity(city, localSize);
        }

        attachUser(hotItems);
        attachUser(latestItems);
        attachUser(localItems);

        return new HomeRecommendVo(hotItems, latestItems, localItems);
    }

    @Override
    public List<IdleItemModel> getContentBasedRecommendations(Long idleId, Integer limit) {
        if (idleId == null) {
            return Collections.emptyList();
        }
        IdleItemModel source = idleItemDao.selectByPrimaryKey(idleId);
        if (source == null || source.getIdleLabel() == null) {
            return Collections.emptyList();
        }

        int targetSize = normalizeLimit(limit, DEFAULT_CONTENT_LIMIT);
        String searchToken = buildSearchToken(source.getIdleName());

        List<IdleItemModel> result = idleItemDao.listSimilarItems(
                source.getIdleLabel(),
                source.getId(),
                source.getIdlePlace(),
                searchToken,
                targetSize
        );

        if (result.size() < targetSize) {
            List<IdleItemModel> secondary = idleItemDao.listSimilarItems(
                    source.getIdleLabel(),
                    source.getId(),
                    source.getIdlePlace(),
                    null,
                    targetSize
            );
            result = mergeDistinct(result, secondary, targetSize);
        }

        if (result.size() < targetSize) {
            List<IdleItemModel> fallback = idleItemDao.listSimilarItems(
                    source.getIdleLabel(),
                    source.getId(),
                    null,
                    null,
                    targetSize
            );
            result = mergeDistinct(result, fallback, targetSize);
        }

        attachUser(result);
        return result;
    }

    private int normalizeLimit(Integer limit, int defaultValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return limit;
    }

    private String resolveCity(Long userId) {
        if (userId == null) {
            return null;
        }
        List<AddressModel> addresses = addressDao.getAddressByUser(userId);
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        for (AddressModel address : addresses) {
            if (Boolean.TRUE.equals(address.getDefaultFlag()) &&
                address.getCityName() != null && !address.getCityName().isEmpty()) {
                return address.getCityName();
            }
        }
        for (AddressModel address : addresses) {
            if (address.getCityName() != null && !address.getCityName().isEmpty()) {
                return address.getCityName();
            }
        }
        return null;
    }

    private void attachUser(List<IdleItemModel> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        for (IdleItemModel item : items) {
            if (item.getUserId() != null) {
                userIds.add(item.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        List<UserModel> users = userDao.findUserByList(new ArrayList<>(userIds));
        Map<Long, UserModel> userMap = new LinkedHashMap<>();
        for (UserModel user : users) {
            userMap.put(user.getId(), user);
        }
        for (IdleItemModel item : items) {
            item.setUser(userMap.get(item.getUserId()));
        }
    }

    private List<IdleItemModel> mergeDistinct(List<IdleItemModel> base,
                                              List<IdleItemModel> extra,
                                              int limit) {
        LinkedHashMap<Long, IdleItemModel> ordered = new LinkedHashMap<>();
        if (base != null) {
            for (IdleItemModel item : base) {
                if (item.getId() != null && !ordered.containsKey(item.getId())) {
                    ordered.put(item.getId(), item);
                }
            }
        }
        if (extra != null) {
            for (IdleItemModel item : extra) {
                if (ordered.size() >= limit) {
                    break;
                }
                if (item.getId() != null && !ordered.containsKey(item.getId())) {
                    ordered.put(item.getId(), item);
                }
            }
        }
        return new ArrayList<>(ordered.values());
    }

    private String buildSearchToken(String idleName) {
        if (idleName == null || idleName.isEmpty()) {
            return null;
        }
        String cleaned = TOKEN_SANITIZER.matcher(idleName).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        String[] segments = cleaned.split("\\s+");
        if (segments.length == 0) {
            return null;
        }
        String token = segments[0];
        if (token.length() > 8) {
            return token.substring(0, 8);
        }
        return token;
    }
}
