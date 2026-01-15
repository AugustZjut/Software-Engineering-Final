<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div style="min-height: 85vh;">
            <el-tabs v-model="labelName" type="card" @tab-click="handleClick">
                <el-tab-pane label="热门" name="hot"></el-tab-pane>
                <el-tab-pane label="全部" name="0"></el-tab-pane>
                <el-tab-pane label="数码" name="1"></el-tab-pane>
                <el-tab-pane label="家电" name="2"></el-tab-pane>
                <el-tab-pane label="户外" name="3"></el-tab-pane>
                <el-tab-pane label="图书" name="4"></el-tab-pane>
                <el-tab-pane label="其他" name="5"></el-tab-pane>
            </el-tabs>
            <div style="margin: 0 20px;">
                <el-row :gutter="30">
                    <el-col :span="6" v-for="(idle,index) in idleList" :key="idle.id+'-'+index">
                        <div class="idle-card" @click="toDetails(idle)">
                            <el-image
                                :src="idle.imgUrl"
                                style="width: 100%; height: 160px"
                                fit="contain">
                                <template #error>
                                    <div class="image-slot">
                                        <i class="el-icon-picture-outline">无图</i>
                                    </div>
                                </template>
                            </el-image>
                            <div class="idle-title">
                                {{idle.idleName}}
                            </div>
                            <el-row style="margin: 5px 10px;">
                                <el-col :span="12">
                                    <div class="idle-prive">￥{{idle.idlePrice}}</div>
                                </el-col>
                                <el-col :span="12">
                                    <div class="idle-place">{{idle.idlePlace}}</div>
                                </el-col>
                            </el-row>
                            <div class="idle-time">{{idle.timeStr}}</div>
                            <div class="user-info">
                                <el-image
                                        style="width: 30px; height: 30px"
                                        :src="idle.user.avatar"
                                        fit="contain">
                                    <template #error>
                                        <div class="image-slot">
                                            <i class="el-icon-picture-outline">无图</i>
                                        </div>
                                    </template>
                                </el-image>
                                <div class="user-nickname">{{idle.user.nickname}}</div>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
            <div class="fenye" v-if="showPagination">
                <el-pagination
                        background
                        @current-change="handleCurrentChange"
                        v-model:current-page="currentPage"
                        :page-size="pageSize"
                        layout="prev, pager, next, jumper"
                        :total="totalItem">
                </el-pagination>
            </div>
            </div>
            <app-foot></app-foot>
        </app-body>
    </div>
</template>

<script>
    import AppHead from '../common/AppHeader.vue';
    import AppBody from '../common/AppPageBody.vue'
    import AppFoot from '../common/AppFoot.vue'

    export default {
        name: "index",
        components: {
            AppHead,
            AppBody,
            AppFoot
        },
        data() {
            return {
                labelName: '0',
                idleList: [],
                currentPage: 1,
                totalItem: 0,
                pageSize: 8,
                hotCache: []
            };
        },
        created() {
            const { labelName, page } = this.resolveRouteParams(this.$route);
            this.labelName = labelName;
            this.currentPage = page;
            this.findIdleTiem(this.currentPage);
        },
        watch:{
            $route(to){
                const { labelName, page } = this.resolveRouteParams(to);
                this.labelName = labelName;
                this.currentPage = page;
                this.findIdleTiem(page);
            }
        },
        computed: {
            showPagination() {
                return this.labelName !== 'hot' && this.totalItem > this.pageSize;
            }
        },
        methods: {
            resolveRouteParams(route){
                const queries = route.query || {};
                const label = queries.labelName ? String(queries.labelName) : '0';
                if(label === 'hot'){
                    return { labelName: 'hot', page: 1 };
                }
                const pageParam = parseInt(queries.page, 10);
                const safePage = Number.isInteger(pageParam) && pageParam > 0 ? pageParam : 1;
                return { labelName: label, page: safePage };
            },
            findIdleTiem(page){
                const loading = this.$loading({
                    lock: true,
                    text: '加载数据中',
                    // spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0)'
                });
                if(this.labelName === 'hot'){
                    this.fetchHotList(loading);
                    return;
                }
                const labelValue = parseInt(this.labelName, 10);
                const isCategory = Number.isInteger(labelValue) && labelValue > 0;
                const request = isCategory
                    ? this.$api.findIdleTiemByLable({
                        idleLabel: labelValue,
                        page: page,
                        nums: this.pageSize
                    })
                    : this.$api.findIdleTiem({
                        page: page,
                        nums: this.pageSize
                    });

                request.then(res => {
                    const listSource = res && res.data && Array.isArray(res.data.list) ? res.data.list : [];
                    const list = this.decorateIdleList(listSource);
                    this.idleList = list;
                    this.totalItem = res && res.data && typeof res.data.count === 'number' ? res.data.count : list.length;
                }).catch(e => {
                    console.log(e)
                }).finally(()=>{
                    loading.close();
                });
            },
            fetchHotList(loading){
                if(this.hotCache.length){
                    this.idleList = [...this.hotCache];
                    this.totalItem = this.hotCache.length;
                    this.currentPage = 1;
                    loading.close();
                    return;
                }
                this.$api.getHomeRecommend({ hotLimit: this.pageSize })
                    .then(res => {
                        if (res.status_code === 1 && res.data && Array.isArray(res.data.hotItems)) {
                            const list = this.decorateIdleList(res.data.hotItems);
                            this.hotCache = list;
                            this.idleList = [...list];
                            this.totalItem = list.length;
                        } else {
                            this.idleList = [];
                            this.totalItem = 0;
                        }
                        this.currentPage = 1;
                    })
                    .catch(() => {
                        this.idleList = [];
                        this.totalItem = 0;
                    })
                    .finally(() => {
                        loading.close();
                    });
            },
            decorateIdleList(list){
                if(!Array.isArray(list)){
                    return [];
                }
                return list.map(item => {
                    const copy = {...item};
                    try {
                        const pictures = JSON.parse(copy.pictureList || '[]');
                        copy.imgUrl = Array.isArray(pictures) && pictures.length > 0 ? pictures[0] : '';
                    } catch (e) {
                        copy.imgUrl = '';
                    }
                    if(copy.releaseTime){
                        copy.timeStr = copy.releaseTime.substring(0, 10) + ' ' + copy.releaseTime.substring(11, 19);
                    }else{
                        copy.timeStr = '';
                    }
                    if(copy.user){
                        copy.user = {
                            ...copy.user,
                            avatar: copy.user.avatar || '',
                            nickname: copy.user.nickname || ''
                        };
                    }else{
                        copy.user = {
                            avatar: '',
                            nickname: ''
                        };
                    }
                    return copy;
                });
            },
            handleClick() {
                if(this.labelName === 'hot'){
                    this.$router.replace({query: {labelName: 'hot'}});
                }else{
                    this.$router.replace({query: {page: 1,labelName:this.labelName}});
                }
            },
            handleCurrentChange(val) {
                if(this.labelName === 'hot'){
                    return;
                }
                this.$router.replace({query: {page: val,labelName:this.labelName}});
            },
            toDetails(idle) {
                this.$router.push({path: '/details', query: {id: idle.id}});
            }
        }
    }
</script>

<style scoped>
    .idle-card {
        height: 300px;
        border: #eeeeee solid 1px;
        margin-bottom: 15px;
        cursor: pointer;
    }

    .fenye {
        display: flex;
        justify-content: center;
        height: 60px;
        align-items: center;
    }

    .idle-title {
        font-size: 18px;
        font-weight: 600;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        margin: 10px;
    }

    .idle-prive {
        font-size: 16px;
        color: red;
    }

    .idle-place {
        font-size: 13px;
        color: #666666;
        float: right;
        padding-right: 20px;

    }

    .idle-time {
        color: #666666;
        font-size: 12px;
        margin: 0 10px;
    }

    .user-nickname {
        color: #999999;
        font-size: 12px;
        display: flex;
        align-items: center;
        height: 30px;
        padding-left: 10px;
    }

    .user-info {
        padding: 5px 10px;
        height: 30px;
        display: flex;
    }
</style>