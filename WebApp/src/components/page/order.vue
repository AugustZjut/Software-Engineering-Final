<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="order-page-container">
                <div class="idle-info-container" @click="toDetails(orderInfo.idleItem.id)">
                    <el-image
                            style="width: 150px; height: 150px;"
                            :src="orderInfo.idleItem.imgUrl"
                            fit="cover"></el-image>
                    <div class="idle-info-title">{{orderInfo.userId==userId?'买到的':'卖出的'}}：{{orderInfo.idleItem.idleName}}</div>
                    <div class="idle-info-price">￥{{orderInfo.orderPrice}}</div>

                </div>
                <div class="address-container" @click.stop="selectAddressDialog" :style="orderInfo.userId==userId&&orderInfo.orderStatus===0?'cursor: pointer;':''">
                    <div class="address-title">收货地址: {{addressInfo.consigneeName}} {{addressInfo.consigneePhone}}</div>
                    <div class="address-detials">{{addressInfo.detailAddress}}</div>
                    <el-button v-if="!addressInfo.detailAddress" @click.stop="selectAddressDialog" type="primary" plain>选择收货地址</el-button>
                </div>
                <el-dialog
                        title="选择地址"
                        v-model="addressDialogVisible"
                        width="800px">
                    <el-table
                            stripe
                            empty-text="无地址信息，请先在个人中心添加地址"
                            :data="addressData"
                            style="width: 100%">
                        <el-table-column
                                prop="consigneeName"
                                label="收货人姓名"
                                width="120">
                        </el-table-column>
                        <el-table-column
                                prop="consigneePhone"
                                label="手机号"
                                width="140">
                        </el-table-column>
                        <el-table-column
                                prop="detailAddressText"
                                label="地址">
                        </el-table-column>
                        <el-table-column label=" " width="120">
                            <template #default="scope">
                                <el-button
                                        size="mini"
                                        @click="selectAddress(scope.$index, scope.row)">选择
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-dialog>
                <div class="order-info-container">
                    <div class="order-info-title">订单信息（{{orderStatus[orderInfo.orderStatus]}}）：</div>
                    <div class="order-info-item">编号：{{orderInfo.orderNumber}}</div>
                    <div class="order-info-item">支付状态：{{orderInfo.paymentStatus===0?'未支付':'已支付'}}</div>
                    <div class="order-info-item">支付方式：{{orderInfo.paymentWay}}</div>
                    <div class="order-info-item">创建时间：{{orderInfo.createTime.substring(0, 10) + ' ' +
                        orderInfo.createTime.substring(11, 19)}}
                    </div>
                    <div class="order-info-item">支付时间：{{orderInfo.paymentTime?orderInfo.paymentTime.substring(0, 10) + ' ' +
                        orderInfo.paymentTime.substring(11, 19):''}}
                    </div>
                </div>
                <div class="menu">
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===0" type="danger" plain @click="changeOrderStatus(4,orderInfo)">取消订单</el-button>
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===0" type="primary" plain @click="changeOrderStatus(1,orderInfo)">立即支付</el-button>
                    <el-button v-if="userId==orderInfo.idleItem.userId&&orderInfo.orderStatus===1" type="primary" plain @click="changeOrderStatus(2,orderInfo)">发货</el-button>
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===2" type="primary" plain @click="changeOrderStatus(3,orderInfo)">确认收货</el-button>
                    <el-button v-if="canEvaluate" type="primary" plain @click="openEvaluationDialog">发布评价</el-button>
                </div>
                <div v-if="orderInfo.orderStatus===3" class="evaluation-container">
                    <div class="evaluation-title">交易评价</div>
                    <div v-if="orderEvaluations.length">
                        <div v-for="item in orderEvaluations" :key="item.id || `${item.evaluatorId}-${item.targetId}`" class="evaluation-item">
                            <div class="evaluation-item-header">
                                <span class="evaluation-user">{{ item.evaluator && item.evaluator.nickname ? item.evaluator.nickname : '匿名用户' }}</span>
                                <el-rate :model-value="item.score" disabled></el-rate>
                            </div>
                            <div class="evaluation-content">{{ item.content || '对方未填写评价内容' }}</div>
                        </div>
                    </div>
                    <div v-else class="evaluation-empty">暂无评价</div>
                </div>
                <el-dialog
                        title="发布评价"
                        v-model="evaluationDialogVisible"
                        width="480px">
                    <div class="evaluation-form">
                        <div class="evaluation-form-item">
                            <span class="evaluation-form-label">评分：</span>
                            <el-rate v-model="evaluationForm.score" :max="5"></el-rate>
                        </div>
                        <el-input
                                v-model="evaluationForm.content"
                                type="textarea"
                                :rows="4"
                                maxlength="512"
                                show-word-limit
                                placeholder="说说你的交易体验吧"></el-input>
                    </div>
                    <template #footer>
                        <el-button @click="evaluationDialogVisible=false">取消</el-button>
                        <el-button type="primary" :loading="submittingEvaluation" @click="submitEvaluation">提交</el-button>
                    </template>
                </el-dialog>
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
        name: "order",
        components: {
            AppHead,
            AppBody,
            AppFoot
        },
        data() {
            return {
                addressDialogVisible:false,
                addressData: [],
                orderStatus: ['待付款', '待发货', '待收货', '已完成', '已取消'],
                orderInfo: {
                    createTime: "",
                    id: 0,
                    idleId: 0,
                    idleItem: {
                        id: '',
                        idleName: '',
                        idleDetails: '',
                        pictureList: [],
                        idlePrice: 0,
                        idlePlace: '',
                        idleLabel: '',
                        idleStatus: -1,
                        userId: '',
                    },
                    orderNumber: "",
                    orderPrice: 0,
                    orderStatus: 0,
                    paymentStatus: 0,
                    paymentTime: "",
                    paymentWay: "",
                    userId: 0
                },
                addressInfo: {
                    id:'',
                    update:false,
                    consigneeName: '',
                    consigneePhone: '',
                    detailAddress: ''
                },
                userId: 0,
                orderEvaluations: [],
                evaluationDialogVisible: false,
                evaluationForm: {
                    score: 5,
                    content: ''
                },
                submittingEvaluation: false
            };
        },
        created() {
            this.userId = Number(this.getCookie('shUserId'));
            console.log('userId',this.userId,this.getCookie('shUserId'));
            let orderId = this.$route.query.id;
            console.log(orderId);
            this.$api.getOrder({
                id: orderId
            }).then(res => {
                console.log(res);
                if (res.status_code === 1) {
                    if (res.data.idleItem) {
                        let imgList = JSON.parse(res.data.idleItem.pictureList);
                        if (imgList.length > 0) {
                            res.data.idleItem.imgUrl = imgList[0];
                        } else {
                            res.data.idleItem.imgUrl = '';
                        }
                    } else {
                        res.data.idleItem = {
                            idleName: '',
                            imgUrl: ''
                        }
                    }
                    this.orderInfo = res.data;
                    this.$api.getOrderAddress({
                        orderId:this.orderInfo.id
                    }).then(res=>{
                        if(res.data){
                            this.addressInfo= res.data;
                            this.addressInfo.update=true;
                        }else{
                            this.getAddressData();
                        }
                    })
                    this.refreshEvaluations();
                }
            })
        },
        computed: {
            isBuyer() {
                return Number(this.userId) === Number(this.orderInfo.userId);
            },
            isSeller() {
                const sellerId = this.orderInfo.idleItem ? this.orderInfo.idleItem.userId : null;
                return Number(this.userId) === Number(sellerId);
            },
            hasSubmittedEvaluation() {
                return this.orderEvaluations.some(item => Number(item.evaluatorId) === Number(this.userId));
            },
            canEvaluate() {
                return (this.isBuyer || this.isSeller) && this.orderInfo.orderStatus === 3 && !this.hasSubmittedEvaluation;
            }
        },
        methods: {
            getCookie(cname){
                var name = cname + "=";
                var ca = document.cookie.split(';');
                for(var i=0; i<ca.length; i++)
                {
                    var c = ca[i].trim();
                    if (c.indexOf(name)===0) return c.substring(name.length,c.length);
                }
                return "0";
            },
            toDetails(id) {
                this.$router.replace({path: 'details', query: {id: id}});
            },
            selectAddressDialog(){
                if(this.orderInfo.userId==this.userId&&this.orderInfo.orderStatus===0){
                    this.addressDialogVisible=true;
                    if(this.addressData.length===0){
                        this.getAddressData();
                    }
                }
            },
            getAddressData(){
                this.$api.getAddress().then(res => {
                    if (res.status_code === 1) {
                        let data = res.data;
                        for (let i = 0; i < data.length; i++) {
                            data[i].detailAddressText = data[i].provinceName + data[i].cityName + data[i].regionName + data[i].detailAddress;
                        }
                        console.log(data);
                        this.addressData = data;
                        if(!this.addressInfo.update){
                            for(let i=0;i<data.length;i++){
                                if(data[i].defaultFlag){
                                    this.selectAddress(i,data[i]);
                                }
                            }
                        }
                    }
                })
            },
            selectAddress(i,item){
                this.addressDialogVisible=false;
                console.log(item,this.addressInfo);
                this.addressInfo.consigneeName=item.consigneeName;
                this.addressInfo.consigneePhone=item.consigneePhone;
                this.addressInfo.detailAddress=item.detailAddressText;
                if(this.addressInfo.update){
                    this.$api.updateOrderAddress({
                        id:this.addressInfo.id,
                        consigneeName:item.consigneeName,
                        consigneePhone:item.consigneePhone,
                        detailAddress:item.detailAddressText
                    })
                }else{
                    this.$api.addOrderAddress({
                        orderId:this.orderInfo.id,
                        consigneeName:item.consigneeName,
                        consigneePhone:item.consigneePhone,
                        detailAddress:item.detailAddressText
                    }).then(res=>{
                        if(res.status_code===1){
                            this.addressInfo.update=true;
                            this.addressInfo.id=res.data.id;
                        }else {
                            this.$message.error(res.msg)
                        }
                    })
                }

            },
            changeOrderStatus(orderStatus, orderInfo) {
                if (orderStatus === 1) {
                    console.log('zhifu');
                    if(!this.addressInfo.detailAddress){
                        this.$message.error('请选择地址！')
                    }else{
                        this.$confirm('模拟支付宝支付，是否确认支付', '支付订单', {
                            confirmButtonText: '支付',
                            cancelButtonText: '取消',
                            type: 'warning'
                        }).then(() => {
                            this.$api.updateOrder({
                                id: orderInfo.id,
                                orderStatus: orderStatus,
                                paymentStatus: 1,
                                paymentWay: '支付宝',
                            }).then(res => {
                                if (res.status_code === 1) {
                                    this.$message({
                                        message: '支付成功！',
                                        type: 'success'
                                    });
                                    this.orderInfo.orderStatus = orderStatus;
                                    this.orderInfo.paymentStatus = 1;
                                    this.orderInfo.paymentWay = '支付宝';
                                    this.orderInfo.paymentTime = res.data.paymentTime;
                                    this.refreshEvaluations();
                                }
                            })
                        }).catch(() => {
                        });
                    }
                } else {
                    this.$api.updateOrder({
                        id: orderInfo.id,
                        orderStatus: orderStatus,
                    }).then(res => {
                        if (res.status_code === 1) {
                            this.$message({
                                message: '操作成功！',
                                type: 'success'
                            });
                            this.orderInfo.orderStatus = orderStatus;
                            if (orderStatus === 3) {
                                this.refreshEvaluations();
                            }
                        }
                    })
                }
            },
            refreshEvaluations() {
                if (!this.orderInfo.id) {
                    return;
                }
                this.$api.getOrderEvaluations({
                    orderId: this.orderInfo.id
                }).then(res => {
                    if (res.status_code === 1) {
                        const list = Array.isArray(res.data) ? res.data : [];
                        this.orderEvaluations = list;
                        const mine = list.find(item => Number(item.evaluatorId) === this.userId);
                        this.evaluationForm.score = mine ? mine.score : 5;
                        this.evaluationForm.content = '';
                        const hasTargetUpdate = list.some(item => Number(item.targetId) === this.userId);
                        if (hasTargetUpdate) {
                            this.$api.getUserInfo().then(infoRes => {
                                if (infoRes.status_code === 1) {
                                    if (infoRes.data.signInTime) {
                                        infoRes.data.signInTime = infoRes.data.signInTime.substring(0, 10);
                                    }
                                    this.$globalData.userInfo = infoRes.data;
                                }
                            });
                        }
                    }
                });
            },
            openEvaluationDialog() {
                this.evaluationDialogVisible = true;
                this.evaluationForm.content = '';
                this.evaluationForm.score = 5;
            },
            submitEvaluation() {
                if (!this.orderInfo.id) {
                    return;
                }
                if (this.submittingEvaluation) {
                    return;
                }
                this.submittingEvaluation = true;
                this.$api.submitEvaluation({
                    orderId: this.orderInfo.id,
                    score: this.evaluationForm.score,
                    content: this.evaluationForm.content
                }).then(res => {
                    if (res.status_code === 1) {
                        this.$message.success('评价成功');
                        this.evaluationDialogVisible = false;
                        this.refreshEvaluations();
                    } else {
                        this.$message.error(res.msg || '评价失败');
                    }
                }).finally(() => {
                    this.submittingEvaluation = false;
                });
            }
        }

    }
</script>

<style scoped>
    .order-page-container {
        min-height: 85vh;
    }

    .idle-info-container {
        width: 100%;
        display: flex;
        border-bottom: 20px solid #f6f6f6;
        padding: 20px;
        cursor: pointer;
    }

    .idle-info-title {
        font-size: 18px;
        font-weight: 600;
        max-width: 750px;
        margin-left: 10px;
    }

    .idle-info-price {
        font-size: 18px;
        color: red;
        margin-left: 10px;
    }

    .address-container {
        min-height: 60px;
        padding: 20px;
        border-bottom: 20px solid #f6f6f6;

    }

    .address-title {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 10px;
    }

    .address-detials {
        font-size: 16px;
        color: #444444;
    }

    .order-info-container {
        padding: 20px;
    }

    .order-info-item {
        margin: 10px 0;
        font-size: 14px;
        color: #444444;
    }

    .menu {
        margin-left: 20px;
    }

    .evaluation-container {
        margin: 20px;
        padding: 20px;
        background: #fafafa;
        border-radius: 8px;
    }

    .evaluation-title {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 12px;
    }

    .evaluation-item {
        padding: 12px 0;
        border-bottom: 1px solid #eee;
    }

    .evaluation-item:last-child {
        border-bottom: none;
    }

    .evaluation-item-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 6px;
    }

    .evaluation-user {
        font-weight: 500;
        color: #333;
    }

    .evaluation-content {
        font-size: 14px;
        color: #555;
        white-space: pre-wrap;
    }

    .evaluation-empty {
        font-size: 14px;
        color: #888;
    }

    .evaluation-form {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }

    .evaluation-form-item {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .evaluation-form-label {
        font-size: 14px;
        color: #333;
    }
</style>