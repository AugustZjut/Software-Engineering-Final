package com.second.hand.trading.server.vo;

import com.second.hand.trading.server.model.IdleItemModel;

import java.util.Collections;
import java.util.List;

public class HomeRecommendVo {

    private List<IdleItemModel> hotItems;
    private List<IdleItemModel> latestItems;
    private List<IdleItemModel> localItems;

    public HomeRecommendVo() {
        this.hotItems = Collections.emptyList();
        this.latestItems = Collections.emptyList();
        this.localItems = Collections.emptyList();
    }

    public HomeRecommendVo(List<IdleItemModel> hotItems,
                           List<IdleItemModel> latestItems,
                           List<IdleItemModel> localItems) {
        this.hotItems = hotItems;
        this.latestItems = latestItems;
        this.localItems = localItems;
    }

    public List<IdleItemModel> getHotItems() {
        return hotItems;
    }

    public void setHotItems(List<IdleItemModel> hotItems) {
        this.hotItems = hotItems;
    }

    public List<IdleItemModel> getLatestItems() {
        return latestItems;
    }

    public void setLatestItems(List<IdleItemModel> latestItems) {
        this.latestItems = latestItems;
    }

    public List<IdleItemModel> getLocalItems() {
        return localItems;
    }

    public void setLocalItems(List<IdleItemModel> localItems) {
        this.localItems = localItems;
    }
}
